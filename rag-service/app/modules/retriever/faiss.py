import logging
import faiss

from langchain_community.vectorstores import FAISS
from langchain_core.embeddings import Embeddings
from langchain_community.docstore import InMemoryDocstore

logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)


class Faiss:
    vector_store_dir = '/app/vector-store'

    def __init__(self, embedding_model: Embeddings):
        self.embedding_model = embedding_model
        self.vectorstore = self._load_or_create_index()

    def _load_or_create_index(self) -> FAISS:
        try:
            logger.info(f"FAISS 인덱스를 로드합니다. 경로: {self.vector_store_dir}")
            return FAISS.load_local(
                folder_path=self.vector_store_dir,
                embeddings=self.embedding_model,
                allow_dangerous_deserialization=True
            )
        except Exception as e:
            logger.warning(f"FAISS 인덱스 로드 실패: {e}. 비어있는 새 인덱스를 생성합니다.")

            dummy_embedding = self.embedding_model.embed_query("test")
            dimension = len(dummy_embedding)

            empty_index = faiss.IndexFlatL2(dimension)

            return FAISS(
                embedding_function=self.embedding_model,
                index=empty_index,
                docstore=InMemoryDocstore({}),
                index_to_docstore_id={}
            )

    def get_retriever(self):
        return self.vectorstore.as_retriever()

    def reload_retriever(self):
        self.vectorstore = FAISS.load_local(
            folder_path=self.vector_store_dir,
            embeddings=self.vectorstore.embeddings,
            allow_dangerous_deserialization=True
        )
