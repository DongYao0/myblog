from fastapi import FastAPI

from app.api.ai import router as ai_router
from app.api.health import router as health_router
from app.core.settings import settings

app = FastAPI(title=settings.service_name)
app.include_router(health_router)
app.include_router(ai_router)
