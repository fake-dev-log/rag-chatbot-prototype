from langchain_ollama import ChatOllama


class Ollama:

    def __init__(self, model=None):
        if model is None:
            model = "gpt-oss:20b"
        self.ollama_model = ChatOllama(
            model=model,
            temperature=0.0,
        )

    def get_model(self) -> ChatOllama:
        return self.ollama_model
