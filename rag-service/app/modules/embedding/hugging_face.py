import logging
import os

from langchain_huggingface import HuggingFaceEmbeddings
import torch


CACHE_FOLDER = os.getenv("EMBEDDING_CACHE_FOLDER", None)
logger = logging.getLogger(__name__)


def _get_device():
    if torch.cuda.is_available():
        logger.info(f"CUDA GPU available: {torch.cuda.get_device_name(0)}")
        logger.info(f"torch version: {torch.__version__}")
        logger.info(f"CUDA version: {torch.version.cuda}")
        logger.info(f"cuDNN enabled: {torch.backends.cudnn.enabled}")
        return "cuda"
    elif torch.backends.mps.is_available() and torch.backends.mps.is_built():
        logger.info("MPS (Metal Performance Shaders) available for Apple Silicon.")
        return "mps"
    else:
        logger.info("No GPU available, using CPU.")
        return "cpu"


class HuggingFace:

    def __init__(self):
        device = _get_device()
        self.embeddings = HuggingFaceEmbeddings(
            model_name="BAAI/bge-m3",
            model_kwargs={"device": device},
            cache_folder=CACHE_FOLDER,
        )

    def get_embedding(self):
        return self.embeddings


hugging_face = HuggingFace()
