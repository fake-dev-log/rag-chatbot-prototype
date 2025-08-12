from langchain_community.vectorstores import FAISS
from langchain_core.embeddings import Embeddings


class Faiss:
    faiss_index_dir = "./faiss_index"

    def __init__(self, embedding_model: Embeddings):
        self.vectorstore = FAISS.load_local(
            folder_path=self.faiss_index_dir,
            embeddings=embedding_model,
            allow_dangerous_deserialization=True
        )

    def get_retriever(self):
        return self.vectorstore.as_retriever()

    def reload_retriever(self):
        self.vectorstore = FAISS.load_local(
            folder_path=self.faiss_index_dir,
            embeddings=self.vectorstore.embeddings,
            allow_dangerous_deserialization=True
        )
