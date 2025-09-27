from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import PromptTemplate
from app.modules.llm import Ollama
from app.title_generator.schemas import TitleGenerationRequest

# Prompt template for generating a concise title
TITLE_PROMPT_TEMPLATE = """
Based on the following first turn of a conversation, generate a short and descriptive title in 5 words or less.
The title should be in the same language as the conversation.

CONVERSATION:
User: {question}
AI: {answer}

TITLE:
"""


class TitleGenerationService:
    """
    Manages the logic for generating a chat title.
    """
    def __init__(self):
        """Initializes the title generation chain."""
        prompt = PromptTemplate.from_template(TITLE_PROMPT_TEMPLATE)
        llm = Ollama().get_model()

        self.chain = prompt | llm | StrOutputParser()

    def generate_title(self, request: TitleGenerationRequest) -> str:
        """
        Generates a new title using the LLM chain.
        """
        title = self.chain.invoke({
            "question": request.question,
            "answer": request.answer,
        })
        # The LLM might add quotes around the title, so we strip them.
        return title.strip().strip('"\'')


# Singleton instance
title_generation_service = TitleGenerationService()
