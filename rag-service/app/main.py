import logging

from fastapi import FastAPI
import uvicorn

from .extensions.logs import LOGGING_CONFIG
from .chat import router as chat_router
from .retriever.routes import router as retriever_router

logger = logging.getLogger(__name__)

app = FastAPI()
app.include_router(chat_router)
app.include_router(retriever_router)


@app.get("/health")
async def health_check():
    return {"status": "ok"}


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000, log_config=LOGGING_CONFIG)
