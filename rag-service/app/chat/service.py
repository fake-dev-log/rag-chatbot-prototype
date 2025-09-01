import json
import logging

logger = logging.getLogger(__name__)

from app.chat.schemas import SourceDocument
from app.modules.embedding import hugging_face
from app.modules.llm import Ollama
from app.modules.prompt.prompt_loader import prompt_loader # Import the prompt loader
from app.modules.retriever import Faiss

from langchain_core.output_parsers import StrOutputParser


def format_ndjson(obj: dict) -> str:
    """Formats a dictionary as a Newline Delimited JSON string."""
    return json.dumps(obj, ensure_ascii=False) + "\n"


class ChatService:
    """
    Manages the core logic of the RAG chatbot, including the retrieval and generation chain.
    """
    ready = False

    def __init__(self):
        """Initializes the RAG chain and its components."""
        # Use the PromptLoader to get the prompt template.
        self.prompt = prompt_loader.get_prompt("default") # Assuming "default" is the name of the prompt to use
        self.llm = Ollama().get_model() # Commented out for testing without Ollama
        embedding = hugging_face.get_embedding()

        self.faiss_manager = Faiss(embedding)
        self.retriever = self.faiss_manager.get_retriever()

        self.chain = (
                {
                    "context": lambda x: x["context"],
                    "question": lambda x: x["question"]
                }
                | self.prompt
                | self.llm
                | StrOutputParser()
        )
        self.ready = True

    def reload_retriever(self):
        """
        Reloads the FAISS vector store retriever from disk.
        """
        self.ready = False
        self.faiss_manager.reload_retriever()
        self.retriever = self.faiss_manager.get_retriever()
        self.ready = True

    def apply_prompt(self, name: str, template_content: str):
        """
        Applies a new prompt by invalidating the cache and reloading the RAG chain.
        """
        self.ready = False
        # Directly update the cache with the provided content
        prompt_loader.cache[name] = template_content
        self.prompt = prompt_loader.get_prompt(name) # This will now get it from cache

        self.chain = (
                {
                    "context": lambda x: x["context"],
                    "question": lambda x: x["question"]
                }
                | self.prompt
                | self.llm
                | StrOutputParser()
        )
        self.ready = True
        logger.info(f"New prompt '{name}' has been applied.")

    def stream(self, query: str):
        """
        Handles a user query by streaming the response from the RAG chain.
        """
        retrieved_docs = self.retriever.invoke(query)

        if not retrieved_docs:
            chain_input = {"context": None, "question": query}
            for chunk in self.chain.stream(chain_input):
                yield format_ndjson({"type": "token", "data": chunk})
        else:
            sources = [
                SourceDocument(
                    file_name=doc.metadata.get('file_name', 'N/A').split('_', 1)[-1],
                    title=str(doc.metadata.get('title', 'N/A')),
                    page_number=doc.metadata.get('page_number', 0),
                    snippet=doc.page_content,
                ).model_dump()
                for doc in retrieved_docs
            ]

            chain_input = {"context": retrieved_docs, "question": query}
            for chunk in self.chain.stream(chain_input):
                yield format_ndjson({"type": "token", "data": chunk})

            yield format_ndjson({"type": "sources", "data": sources})