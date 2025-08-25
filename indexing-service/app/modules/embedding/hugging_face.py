import os

from langchain_huggingface import HuggingFaceEmbeddings
import torch

import logging

CACHE_FOLDER = os.getenv("EMBEDDING_CACHE_FOLDER", None)
logger = logging.getLogger(__name__)


def _has_cuda():
    try:
        logger.info(torch.cuda.get_device_name(torch.cuda.current_device()))
        logger.info("torch version:", torch.__version__)
        logger.info("CUDA version:", torch.version.cuda)
        logger.info("cuDNN enabled:", torch.backends.cudnn.enabled)
        logger.info("CUDA available:", torch.cuda.is_available())

        if torch.cuda.is_available():
            logger.info("GPU device:", torch.cuda.get_device_name(0))
            return True
        else:
            return False

    except Exception:
        logger.error("No GPU available, using CPU instead")
        return False


class HuggingFace:

    def __init__(self):
        self.embeddings = HuggingFaceEmbeddings(
            model_name="BAAI/bge-m3",
            model_kwargs={"device": "cuda" if _has_cuda() else "cpu"},
            cache_folder=CACHE_FOLDER,
        )

    def get_embedding(self):
        return self.embeddings


hugging_face = HuggingFace()
