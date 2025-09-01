import logging
import os
import httpx
from cachetools import cached, TTLCache
from langchain_core.prompts import PromptTemplate

logger = logging.getLogger(__name__)


class PromptLoader:
    """
    Manages loading and caching of prompt templates from the core-api.
    """
    def __init__(self):
        self.core_api_url = os.environ.get("CORE_API_URL", "http://core-api:8080")
        # Cache up to 10 prompts for 1 hour.
        self.cache = TTLCache(maxsize=10, ttl=3600)

    def _fetch_prompt_from_api(self, name: str) -> str:
        """
        Fetches a single prompt template by name from the core-api.
        This method is cached to avoid repeated API calls.
        """
        # Manual cache lookup
        if name in self.cache:
            return self.cache[name]

        try:
            # This is a simplified example. In a real-world scenario, you would need
            # to handle authentication between services.
            response = httpx.get(f"{self.core_api_url}/admin/prompts/name/{name}") # Assuming an endpoint to fetch by name
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
        # For this prototype, we assume a single default prompt is used.
        # The logic can be extended to fetch different prompts by name.
        template_content = self._fetch_prompt_from_api(name)
        return PromptTemplate(
            input_variables=["context", "question"],
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
