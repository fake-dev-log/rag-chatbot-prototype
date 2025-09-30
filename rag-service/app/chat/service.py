import json
import logging

from app.chat.schemas import SourceDocument
from app.modules.llm import Ollama
from app.modules.prompt.prompt_loader import prompt_loader
from app.modules.retriever import retriever_provider  # Import the provider
from langchain_core.output_parsers import StrOutputParser
from langchain_core.runnables import RunnablePassthrough

logger = logging.getLogger(__name__)


def format_ndjson(obj: dict) -> str:
    """Formats a dictionary as a Newline Delimited JSON string."""
    return json.dumps(obj, ensure_ascii=False) + "\n"


class ChatService:
    """
    Manages the core logic of the RAG chatbot, including the retrieval and generation chain.
    """

    def __init__(self):
        """Initializes the RAG chain components, holding the vector store for dynamic retriever creation."""
        self.prompt = prompt_loader.get_prompt("default")
        self.llm = Ollama().get_model()
        self.vector_store = retriever_provider.get_vector_store()  # Get the vector store instance
        logger.info("ChatService initialized with vector store.")

    def apply_prompt(self, name: str, template_content: str):
        """
        Applies a new prompt by invalidating the cache.
        The chain is now built dynamically in the `stream` method.
        """
        prompt_loader.cache[name] = template_content
        self.prompt = prompt_loader.get_prompt(name)
        logger.info(f"New prompt '{name}' has been applied.")

    def stream(self, query: str, chat_history: str | None = None, category: str | None = None):
        """
        Handles a user query by streaming the response from the RAG chain.
        Dynamically builds the retriever and chain based on the request.
        """
        chat_history = chat_history or "No conversation history yet."

        # 1. Dynamically configure the retriever with search kwargs for filtering
        search_kwargs = {'k': 2}
        if category:
            search_kwargs["filter"] = [{"term": {"metadata.category": category}}]
        
        request_retriever = self.vector_store.as_retriever(search_kwargs=search_kwargs)

        # 2. Build the chain for this specific request
        chain_with_context = RunnablePassthrough.assign(
            context=(lambda x: x['question']) | request_retriever
        )
        request_chain = (
                chain_with_context
                | self.prompt
                | self.llm
                | StrOutputParser()
        )

        # 3. Get source documents separately for the client response
        retrieved_docs = request_retriever.invoke(query)

        # 4. Stream the response from the chain
        chain_input = {"question": query, "chat_history": chat_history}
        for chunk in request_chain.stream(chain_input):
            yield format_ndjson({"type": "token", "data": chunk})

        # 5. Yield the source documents at the end
        if retrieved_docs:
            sources = [
                SourceDocument(
                    file_name=doc.metadata.get('file_name', 'N/A'),
                    title=str(doc.metadata.get('title', 'N/A')),
                    page_number=doc.metadata.get('page_number', 0),
                    snippet=doc.page_content,
                ).model_dump(by_alias=True)
                for doc in retrieved_docs
            ]
            yield format_ndjson({"type": "sources", "data": sources})


# Singleton instance of the ChatService, used by chat routes.
service = ChatService()
