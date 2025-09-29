from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import PromptTemplate
from app.modules.llm import Ollama
from app.summarize.schemas import SummarizationRequest

# A simple prompt template for summarization
SUMMARY_PROMPT_TEMPLATE = """
You are an expert in summarizing conversations.
Your task is to create a concise summary of the following text, which includes a previous summary and the latest user question and AI answer.
The summary should be a single, dense paragraph.
The summary should be in the same language as the conversation.

Previous summary:
{previous_summary}

---
New conversation:
User: {new_question}
AI: {new_answer}
---

Based on the above, create a new, updated summary of the entire conversation.
New summary:
"""


class SummarizationService:
    """
    Manages the logic for summarizing conversations.
    """

    def __init__(self):
        """Initializes the summarization chain."""
        prompt = PromptTemplate.from_template(SUMMARY_PROMPT_TEMPLATE)
        llm = Ollama().get_model()

        self.chain = prompt | llm | StrOutputParser()

    def summarize(self, request: SummarizationRequest) -> str:
        """
        Generates a new summary using the LLM chain.
        """
        previous_summary = request.previous_summary if request.previous_summary else "N/A"

        new_summary = self.chain.invoke({
            "previous_summary": previous_summary,
            "new_question": request.new_question,
            "new_answer": request.new_answer,
        })

        return new_summary


summarization_service = SummarizationService()