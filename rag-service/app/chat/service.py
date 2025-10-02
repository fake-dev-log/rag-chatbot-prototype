import json
import logging

from app.chat.schemas import SourceDocument
from app.modules.llm import Ollama
from app.modules.prompt.prompt_loader import prompt_loader
from app.modules.retriever import retriever_provider
from app.modules.retriever.hybrid_retriever import hybrid_search
from langchain_core.output_parsers import StrOutputParser
from langchain_core.runnables import RunnablePassthrough
from langchain_core.documents import Document

logger = logging.getLogger(__name__)


def format_ndjson(obj: dict) -> str:
    """Formats a dictionary as a Newline Delimited JSON string."""
    return json.dumps(obj, ensure_ascii=False) + "\n"


def format_docs(docs: list[Document]):
    return "\n\n".join(doc.page_content for doc in docs)


class ChatService:
    """
    Manages the core logic of the RAG chatbot, including the retrieval and generation chain.
    """

    def __init__(self):
        """Initializes the RAG chain components, holding the vector store for dynamic retriever creation."""
        self.prompt = prompt_loader.get_prompt("default")
        self.llm = Ollama().get_model()
        self.vector_store = retriever_provider.get_vector_store()
        logger.info("ChatService initialized with vector store.")

    def apply_prompt(self, name: str, template_content: str):
        """
        Applies a new prompt by invalidating the cache.
        """
        prompt_loader.cache[name] = template_content
        self.prompt = prompt_loader.get_prompt(name)
        logger.info(f"New prompt '{name}' has been applied.")

    async def stream(self, query: str, chat_history: str | None = None, category: str | None = None):
        """
        Handles a user query by streaming the response from the RAG chain.
        Uses a manual hybrid search to retrieve documents, bypassing Elasticsearch license restrictions.
        """

        chat_history = chat_history or "No conversation history yet."

        # 1. Retrieve documents using the manual hybrid search function
        retrieved_docs = await hybrid_search(
            query=query,
            vector_store=self.vector_store,
            category=category,
            k=2
        )

        # 2. Build the chain with the retrieved context
        chain_with_context = RunnablePassthrough.assign(
            context=lambda _: format_docs(retrieved_docs)
        )
        request_chain = (
            chain_with_context
            | self.prompt
            | self.llm
            | StrOutputParser()
        )

        # 3. Stream the response from the chain
        chain_input = {"question": query, "chat_history": chat_history}
        async for chunk in request_chain.astream(chain_input):
            yield format_ndjson({"type": "token", "data": chunk})

        # 4. Yield the source documents at the end
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
