import os

from langchain_openai import ChatOpenAI


class OpenAI:

    def __init__(self):
        self.openai_model = ChatOpenAI(
            model=os.getenv("OPENAI_MODEL", "gpt-4o-mini"),
            temperature=0.0,
            max_tokens=1024,
        )

    def get_model(self) -> ChatOpenAI:
        return self.openai_model
