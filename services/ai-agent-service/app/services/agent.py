import ast
import operator
import re
from dataclasses import dataclass, field

try:
    from langchain_core.tools import tool
except Exception:  # pragma: no cover - keeps local demo usable before deps install
    def tool(func):
        func.name = func.__name__
        return func


@tool
def calculator(expression: str) -> str:
    """Calculate a simple arithmetic expression."""
    return str(_safe_eval(expression))


@tool
def article_retriever(query: str) -> str:
    """Retrieve matching article snippets from the local article memory."""
    return query


@dataclass
class AgentResponse:
    answer: str
    tools_used: list[str] = field(default_factory=list)
    context: list[str] = field(default_factory=list)


class BlogAgent:
    def __init__(self) -> None:
        self._articles: dict[str, str] = {}

    def add_article(self, article_id: str, content: str) -> None:
        self._articles[article_id] = content

    def chat(self, message: str) -> AgentResponse:
        expression = _extract_expression(message)
        if expression:
            return AgentResponse(answer=calculator.invoke({"expression": expression}) if hasattr(calculator, "invoke") else calculator(expression), tools_used=["calculator"])

        matches = self._retrieve(message)
        if matches:
            context = [content for _, content in matches]
            return AgentResponse(answer=context[0], tools_used=["article_retriever"], context=context)

        return AgentResponse(
            answer="AI Agent 已启用工具调用、短期记忆与文章 RAG；请提供文章内容或明确计算任务。",
            tools_used=[],
            context=[],
        )

    def _retrieve(self, query: str) -> list[tuple[str, str]]:
        keywords = {word.lower() for word in re.findall(r"[A-Za-z0-9]+", query)}
        results = []
        for article_id, content in self._articles.items():
            haystack = content.lower()
            if any(keyword in haystack for keyword in keywords):
                results.append((article_id, content))
        return results[:3]


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
