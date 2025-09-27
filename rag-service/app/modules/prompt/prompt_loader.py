import logging
import os
import httpx
import uuid
from cachetools import TTLCache
from langchain_core.prompts import PromptTemplate

from app.modules.redis_client import client

logger = logging.getLogger(__name__)


class PromptLoader:
    """
    Manages loading and caching of prompt templates from the core-api.
    """
    def __init__(self):
        self.core_api_url = os.environ.get("CORE_API_URL", "http://core-api:8080")
        # Cache up to 10 prompts for 1 hour.
        self.cache = TTLCache(maxsize=10, ttl=3600)
        self.redis_client = client

    def _fetch_prompt_from_api(self, name: str) -> str:
        """
        Fetches a single prompt template by name from the core-api.
        This method is cached to avoid repeated API calls.
        """
        # Manual cache lookup
        if name in self.cache:
            return self.cache[name]

        try:
            # Generate one-time key and secret for inter-service authentication
            key = str(uuid.uuid4())
            secret = str(uuid.uuid4())

            # Save to Redis with a short TTL (e.g., 60 seconds)
            self.redis_client.set(key, secret, ex=60)

            headers = {
                "X-API-KEY": key,
                "X-API-SECRET": secret
            }

            response = httpx.get(
                f"{self.core_api_url}/internal/prompts/name/{name}",
                headers=headers
            )
            response.raise_for_status()
            template_content = response.json()["templateContent"]
            # Manual cache store
            self.cache[name] = template_content
            return template_content
        except httpx.HTTPError as e:
            logger.error(f"Failed to fetch prompt '{name}' from core-api: {e}")
            # Fallback to a default template
            return "Context: {context}\nQuestion: {question}\nAnswer:"

    def get_prompt(self, name: str = "default") -> PromptTemplate:
        """
        Gets a prompt template by name, utilizing the cache.
        """
        template_content = self._fetch_prompt_from_api(name)
        # The input variables must now include chat_history
        return PromptTemplate(
            input_variables=["context", "question", "chat_history"],
            template=template_content,
        )

    def clear_cache(self):
        """
        Clears the prompt cache.
        """
        self.cache.clear()
        logger.info("Prompt cache has been cleared.")

    def invalidate_prompt(self, name: str):
        """
        Invalidates a specific prompt from the cache.
        """
        if name in self.cache:
            del self.cache[name]
            logger.info(f"Prompt '{name}' has been invalidated from the cache.")


# Singleton instance
prompt_loader = PromptLoader()