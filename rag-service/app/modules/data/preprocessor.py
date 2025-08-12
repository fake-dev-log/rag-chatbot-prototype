import logging
import os
import re
import uuid
from pathlib import PurePath, Path
from typing import Union

import pandas as pd
from langchain_community.vectorstores import FAISS
from langchain_community.document_loaders import PyPDFLoader
from langchain.text_splitter import RecursiveCharacterTextSplitter

from ..embedding import hugging_face

logger = logging.getLogger(__name__)


def load_pdf(pdf_path: Union[str, PurePath]) -> list:
    loader = PyPDFLoader(pdf_path)
    doc = loader.load()
    return doc


def split_text(doc: list) -> list:
    text_splitter = RecursiveCharacterTextSplitter(
        chunk_size=1000,
        chunk_overlap=200,
        length_function=len
    )
    chunks = text_splitter.split_documents(doc)
    return chunks


def extract_heading(text: str) -> str:
    m = re.match(r"^(?:\d+\.)+\s+.+", text.strip().split("\n")[0])
    return m.group(0) if m else ""


class Preprocessor:
    faiss_index_dir = "./faiss_index"

    def __init__(
            self,
            source_dir: Union[str, PurePath],
            processed_dir: Union[str, PurePath],
    ):
        if not Path(source_dir).exists() or not Path(processed_dir).exists():
            raise FileNotFoundError
        self.source_dir = source_dir
        self.processed_dir = processed_dir
        self.embedding = hugging_face.get_embedding()

        if not Path(self.faiss_index_dir).exists():
            os.makedirs(self.faiss_index_dir)

    def process(self):
        if len(os.listdir(self.faiss_index_dir)) == 0:
            if len(os.listdir(self.processed_dir)) == 0:
                self.preprocess_pdfs()
            else:
                logger.info("Preprocessed files already exist.")
            self.embed_and_index(csv_folder=self.processed_dir, index_path=self.faiss_index_dir)
        else:
            logger.info("FAISS index already exists.")

    def preprocess_pdfs(self) -> None:
        logger.info("Preprocessing PDFs")
        data_path = Path(self.source_dir)
        for file in os.listdir(data_path):
            self.preprocess_pdf(Path.joinpath(data_path, file))
        logger.info("Finished preprocessing PDFs")

    def preprocess_pdf(self, pdf_path: Union[str, PurePath]) -> None:
        file_name = Path(pdf_path).name
        logger.info(f"Preprocessing {file_name}")
        doc = load_pdf(pdf_path)
        chunks = split_text(doc)
        self.save_chunks_as_csv(file_name, chunks)

    def save_chunks_as_csv(self, file_name: str, chunks: list) -> None:
        rows = []
        for chunk in chunks:
            heading = extract_heading(chunk.page_content)
            rows.append({
                "document_id": str(uuid.uuid4()),
                "file_name": file_name,
                "page_number": chunk.metadata.get("page", ""),
                "title": chunk.metadata.get("title", heading),
                "author": chunk.metadata.get("author", ""),
                "page_content": chunk.page_content
            })
        df = pd.DataFrame(rows)
        df.to_csv(f"{self.processed_dir}/{Path(file_name).stem}.csv", index=False, encoding="utf-8-sig")

    def embed_and_index(self, csv_folder: str, index_path: str):
        logger.info("Loading embeddings...")
        # 1. Load all preprocessed CSVs
        dfs = []
        for fname in os.listdir(csv_folder):
            logger.info(fname)
            dfs.append(pd.read_csv(os.path.join(csv_folder, fname)))
        df = pd.concat(dfs, ignore_index=True)

        logger.info("Initializing embeddings...")
        # 2. Initialize the embedding model (Hugging Face BGE-M3)
        embeddings = self.embedding

        logger.info("Building vectorstore...")
        # 3. Build the FAISS vector store
        vectorstore = FAISS.from_texts(
            texts=df["page_content"].tolist(),
            embedding=embeddings,
            metadatas=df.to_dict("records")
        )

        logger.info("Saving index...")
        # 4. Save the FAISS index to disk
        os.makedirs(index_path, exist_ok=True)
        vectorstore.save_local(index_path)
        logger.info(f"FAISS index saved to: {index_path}")
