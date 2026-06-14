from collections.abc import Iterator

from fastapi import APIRouter
from fastapi.responses import StreamingResponse
from pydantic import BaseModel

router = APIRouter(prefix="/api/ai")


class SummaryRequest(BaseModel):
    content: str


class SummaryResponse(BaseModel):
    summary: str


@router.post("/summary")
def summarize(request: SummaryRequest) -> SummaryResponse:
    words = request.content.split()
    return SummaryResponse(summary=" ".join(words[:5]))


@router.get("/chat/stream")
def chat_stream(message: str) -> StreamingResponse:
    def events() -> Iterator[str]:
        for token in message.split() or [""]:
            yield f"event: token\ndata: {token}\n\n"
        yield "event: done\ndata: [DONE]\n\n"

    return StreamingResponse(events(), media_type="text/event-stream")
