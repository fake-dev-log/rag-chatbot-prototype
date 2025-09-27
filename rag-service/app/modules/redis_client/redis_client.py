import os

import redis


class RedisClient:
    def __init__(self):
        self.client = redis.Redis(
            host=os.getenv("REDIS_HOST", "localhost"),
            port=6379,
            db=0,
            decode_responses=True
        )

    def set(self, key, value, **kwargs):
        self.client.set(name=key, value=value, **kwargs)

    def get(self, key):
        return self.client.get(key)

    def get_del(self, key):
        return self.client.getdel(key)


client = RedisClient()
