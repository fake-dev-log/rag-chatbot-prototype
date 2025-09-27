import logging
from logging.config import dictConfig

from fastapi import FastAPI
import uvicorn

from .extensions.logging_config import LOGGING_CONFIG
from .chat import router as chat_router
from .retriever.routes import router as retriever_router
from .prompt.routes import router as prompt_router
from .summarize.routes import router as summarize_router
from .title_generator.routes import router as title_generator_router

dictConfig(LOGGING_CONFIG)

logger = logging.getLogger(__name__)

app = FastAPI()
app.include_router(chat_router, tags=["Chat"])
app.include_router(retriever_router, tags=["Retriever"])
app.include_router(prompt_router, tags=["Prompt"])
app.include_router(summarize_router, tags=["Summarize"])
app.include_router(title_generator_router, tags=["Title Generation"])


@app.get("/health")
async def health_check():
    return {"status": "ok"}


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)