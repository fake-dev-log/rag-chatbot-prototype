from fastapi import Header, HTTPException

from modules.redis_client import client

async def verify_api_key(x_api_key: str = Header(...), x_api_secret: str = Header(...)):
    if not (value := client.get_del(x_api_key)) or value != x_api_secret:
        raise HTTPException(status_code=401, detail="Invalid API Key")