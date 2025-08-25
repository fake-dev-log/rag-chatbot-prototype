from fastapi import FastAPI

import uvicorn

from .extensions.logs import LOGGING_CONFIG
from .indexing import routes as indexing_routes

app = FastAPI()

app.include_router(indexing_routes.router, tags=["Indexing"])


@app.get("/health")
async def health_check():
    return {"status": "ok"}


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8001, log_config=LOGGING_CONFIG)
