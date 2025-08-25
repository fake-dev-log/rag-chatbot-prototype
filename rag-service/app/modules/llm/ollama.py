import os

from langchain_ollama import ChatOllama


class Ollama:

    def __init__(self, model=None):
        llm_api_base = os.environ.get("LLM_API_BASE")

        if not llm_api_base:
            print("WARNING: LLM_API_BASE environment variable not set. Defaulting to localhost.")
            llm_api_base = "http://localhost:11434"

        if model is None:
            model = "gpt-oss:20b"
        self.ollama_model = ChatOllama(
            base_url=llm_api_base,
            model=model,
            temperature=0.0,
        )

    def get_model(self) -> ChatOllama:
        return self.ollama_model
