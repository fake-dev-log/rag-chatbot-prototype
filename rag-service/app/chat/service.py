import json

from app.chat.schemas import SourceDocument
from app.modules.embedding import hugging_face
from app.modules.llm import OpenAI
from app.modules.prompt import MedicalDeviceCyberSecurityTemplate
from app.modules.retriever import Faiss

from langchain_core.output_parsers import StrOutputParser
from langchain_core.runnables import RunnableParallel, RunnablePassthrough


def format_ndjson(obj: dict) -> str:
    return json.dumps(obj, ensure_ascii=False) + "\n"


class ChatService:
    ready = False

    def __init__(self):
        self.prompt = MedicalDeviceCyberSecurityTemplate().get_prompt()
        self.llm = OpenAI().get_model()
        embedding = hugging_face.get_embedding()
        self.retriever = Faiss(embedding).get_retriever()

        self.chain = self.prompt | self.llm | StrOutputParser()
        self.runner = RunnableParallel(
            {
                "question": RunnablePassthrough(),
                "context": self.retriever,
            }
        ).assign(chain=self.chain)
        self.ready = True

    def reload_retriever(self):
        self.ready = False
        self.retriever.reload()
        self.runner = RunnableParallel(
            {
                "question": RunnablePassthrough(),
                "context": self.retriever,
            }
        ).assign(chain=self.chain)
        self.ready = True

    def stream(self, query: str):
        sources = []
        for chunk in self.runner.stream(query):
            if "context" in chunk:
                # 모아 두었다가 마지막에 한 번에 내보냄
                sources.extend([
                    SourceDocument(
                        file_name=doc.metadata['file_name'],
                        title=str(doc.metadata['title']),
                        page_number=doc.metadata['page_number'],
                        snippet=doc.page_content,
                    ).model_dump()
                    for doc in chunk["context"]])
            if "chain" in chunk:
                # 토큰 단위 스트리밍
                yield format_ndjson({"type": "token", "data": chunk["chain"]})

        # 스트림 끝날 때 한 번에 소스 목록 내보내기
        yield format_ndjson({"type": "sources", "data": sources})
