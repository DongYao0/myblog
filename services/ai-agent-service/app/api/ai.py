from collections.abc import Iterator

from fastapi import APIRouter
from fastapi.responses import StreamingResponse
from pydantic import BaseModel

from app.services.agent import AgentResponse, BlogAgent

router = APIRouter(prefix="/api/ai")
agent = BlogAgent()


class SummaryRequest(BaseModel):
    content: str


class SummaryResponse(BaseModel):
    summary: str


class AgentChatRequest(BaseModel):
    message: str
    session_id: str = "default"


class AgentChatResponse(BaseModel):
    answer: str
    tools_used: list[str]
    context: list[str]
    memory: list[str] = []


class AgentDocumentRequest(BaseModel):
    document_id: str
    content: str


@router.post("/summary")
def summarize(request: SummaryRequest) -> SummaryResponse:
    words = request.content.split()
    return SummaryResponse(summary=" ".join(words[:5]))


@router.post("/agent/chat")
def agent_chat(request: AgentChatRequest) -> AgentChatResponse:
    response: AgentResponse = agent.chat(request.message, request.session_id)
    return AgentChatResponse(
        answer=response.answer,
        tools_used=response.tools_used,
        context=response.context,
        memory=response.memory,
    )


@router.post("/agent/documents")
def add_agent_document(request: AgentDocumentRequest) -> dict[str, str]:
    agent.add_article(request.document_id, request.content)
    return {"status": "indexed", "document_id": request.document_id}


@router.get("/chat/stream")
def chat_stream(message: str) -> StreamingResponse:
    def events() -> Iterator[str]:
        for token in message.split() or [""]:
            yield f"event: token\ndata: {token}\n\n"
        yield "event: done\ndata: [DONE]\n\n"

    return StreamingResponse(events(), media_type="text/event-stream")
