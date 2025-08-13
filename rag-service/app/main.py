import logging

from fastapi import FastAPI, HTTPException, Depends
import uvicorn

from app import verify_api_key
from extensions.logs import LOGGING_CONFIG
from chat import router as chatrouter

preprocess_completed = False
logger = logging.getLogger(__name__)

app = FastAPI()
app.include_router(chatrouter)


@app.get("/health")
async def health_check():
    return {"status": "ok"}


@app.get("/ready", dependencies=[Depends(verify_api_key)])
async def readiness_check():
    if preprocess_completed:
        return {"ready": True}
    else:
        # HTTP 503: Service Unavailable
        return HTTPException(status_code=503, detail={"ready": False})


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=5000, log_config=LOGGING_CONFIG)
