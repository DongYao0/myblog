import ast
import operator
import os
import re
from dataclasses import dataclass, field

try:
    from langchain_core.tools import tool
except Exception:  # pragma: no cover
    def tool(func):
        func.name = func.__name__
        return func

from app.services.llm import DeepSeekClient, LlmClient, LocalLlm
from app.services.memory import AgentMemoryStore
from app.services.vector_store import VectorStore


@tool
def calculator(expression: str) -> str:
    """Calculate a simple arithmetic expression."""
    return str(_safe_eval(expression))


@dataclass
class AgentResponse:
    answer: str
    tools_used: list[str] = field(default_factory=list)
    context: list[str] = field(default_factory=list)
    memory: list[str] = field(default_factory=list)


class BlogAgent:
    def __init__(
        self,
        llm: LlmClient | None = None,
        memory: AgentMemoryStore | None = None,
        vectors: VectorStore | None = None,
    ) -> None:
        self.llm = llm or _build_llm()
        self.memory = memory or AgentMemoryStore()
        self.vectors = vectors or VectorStore()

    def add_article(self, article_id: str, content: str) -> None:
        self.vectors.upsert(article_id, content, {"kind": "article"})

    def chat(self, message: str, session_id: str = "default") -> AgentResponse:
        expression = _extract_expression(message)
        if expression:
            answer = calculator.invoke({"expression": expression}) if hasattr(calculator, "invoke") else calculator(expression)
            self._remember(session_id, message, answer)
            return AgentResponse(answer=answer, tools_used=["calculator"], memory=self._memory_text(session_id))

        results = self.vectors.search(message, limit=3)
        context = [item.content for item in results]
        tools = ["article_retriever"] if context else []
        memory = self._memory_text(session_id)
        messages = [
            {
                "role": "system",
                "content": (
                    "You are MyBlog's AI Agent. Answer in Chinese when the user writes Chinese. "
                    "Use the supplied Context and Memory. Context:\n" + "\n---\n".join(context)
                ),
            },
            {"role": "system", "content": "Memory:\n" + "\n".join(memory)},
            {"role": "user", "content": message},
        ]
        answer = self.llm.chat(messages)
        self._remember(session_id, message, answer)
        return AgentResponse(answer=answer, tools_used=tools or ["deepseek_chat"], context=context, memory=memory)

    def _remember(self, session_id: str, message: str, answer: str) -> None:
        self.memory.add(session_id, "user", message)
        self.memory.add(session_id, "assistant", answer)

    def _memory_text(self, session_id: str) -> list[str]:
        return [f"{item.role}: {item.content}" for item in self.memory.recent(session_id)]


def _build_llm() -> LlmClient:
    api_key = os.getenv("DEEPSEEK_API_KEY")
    if api_key:
        return DeepSeekClient(
            api_key=api_key,
            base_url=os.getenv("DEEPSEEK_BASE_URL", "https://api.deepseek.com"),
            model=os.getenv("DEEPSEEK_MODEL", "deepseek-chat"),
        )
    return LocalLlm()


def _extract_expression(message: str) -> str | None:
    normalized = message.replace("×", "*").replace("加", "+").replace("减", "-")
    match = re.search(r"(\d+\s*[+\-*/]\s*\d+)", normalized)
    return match.group(1) if match else None


def _safe_eval(expression: str) -> int | float:
    operators = {
        ast.Add: operator.add,
        ast.Sub: operator.sub,
        ast.Mult: operator.mul,
        ast.Div: operator.truediv,
    }

    def visit(node):
        if isinstance(node, ast.Expression):
            return visit(node.body)
        if isinstance(node, ast.Constant) and isinstance(node.value, (int, float)):
            return node.value
        if isinstance(node, ast.BinOp) and type(node.op) in operators:
            return operators[type(node.op)](visit(node.left), visit(node.right))
        raise ValueError("unsupported expression")

    result = visit(ast.parse(expression, mode="eval"))
    return int(result) if isinstance(result, float) and result.is_integer() else result
