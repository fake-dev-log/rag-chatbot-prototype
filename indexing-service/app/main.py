from logging.config import dictConfig

from fastapi import FastAPI

import uvicorn

from .extensions.logging_config import LOGGING_CONFIG
from .documents import router as documents_router

dictConfig(LOGGING_CONFIG)

app = FastAPI()

app.include_router(documents_router, tags=["Documents"])


@app.get("/health")
async def health_check():
    return {"status": "ok"}


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8001)
