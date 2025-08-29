import logging
from logging.config import dictConfig

from fastapi import FastAPI
import uvicorn

from .extensions.logging_config import LOGGING_CONFIG
from .chat import router as chat_router
from .retriever.routes import router as retriever_router

dictConfig(LOGGING_CONFIG)

logger = logging.getLogger(__name__)

app = FastAPI()
app.include_router(chat_router, tags=["Chat"])
app.include_router(retriever_router, tags=["Retriever"])


@app.get("/health")
async def health_check():
    return {"status": "ok"}


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
