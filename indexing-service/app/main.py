from fastapi import FastAPI

import uvicorn

from extensions.logs import LOGGING_CONFIG

app = FastAPI()

@app.get("/health")
async def health_check():
    return {"status": "ok"}

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=5001, log_config=LOGGING_CONFIG)
