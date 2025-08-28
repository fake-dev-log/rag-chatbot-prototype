import json

from app.chat.schemas import SourceDocument
from app.modules.embedding import hugging_face
from app.modules.llm import Ollama
from app.modules.prompt import MedicalDeviceCyberSecurityTemplate
from app.modules.retriever import Faiss

from langchain_core.output_parsers import StrOutputParser


def format_ndjson(obj: dict) -> str:
    return json.dumps(obj, ensure_ascii=False) + "\n"


class ChatService:
    ready = False

    def __init__(self):
        self.prompt = MedicalDeviceCyberSecurityTemplate().get_prompt()
        self.llm = Ollama().get_model()
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
        self.ready = False
        self.faiss_manager.reload_retriever()
        self.retriever = self.faiss_manager.get_retriever()
        self.ready = True

    def stream(self, query: str):
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

            # 스트림 끝날 때 한 번에 소스 목록 내보내기
            yield format_ndjson({"type": "sources", "data": sources})