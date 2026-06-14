from fastapi import APIRouter

from app.core.settings import settings

router = APIRouter()


@router.get("/health")
def health() -> dict[str, str]:
    return {"service": settings.service_name, "status": "UP"}
