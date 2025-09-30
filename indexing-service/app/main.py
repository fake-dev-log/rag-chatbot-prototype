import logging
from logging.config import dictConfig

from fastapi import FastAPI, Request, Response
from starlette.middleware.base import BaseHTTPMiddleware
import uvicorn

from .extensions.logging_config import LOGGING_CONFIG
from .documents import router as documents_router

dictConfig(LOGGING_CONFIG)

logger = logging.getLogger(__name__)


class LoggingMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next: callable) -> Response:
        logger.info(f"Request: {request.method} {request.url}")
        response = await call_next(request)
        logger.info(f"Response: status_code={response.status_code}")
        return response


app = FastAPI()
app.add_middleware(LoggingMiddleware)

app.include_router(documents_router, tags=["Documents"])


@app.get("/health")
async def health_check():
    return {"status": "ok"}


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8001)
