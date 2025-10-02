import logging
import asyncio
from typing import List
from langchain_core.documents import Document
from langchain_elasticsearch import ElasticsearchStore

logger = logging.getLogger(__name__)


def reciprocal_rank_fusion(search_results: List[List[Document]], k: int = 60) -> List[Document]:
    """
    Applies Reciprocal Rank Fusion to a list of search results.
    This function is executed in the application layer, not in Elasticsearch.
    """
    fused_scores = {}
    for docs in search_results:
        for rank, doc in enumerate(docs):
            # Use a unique identifier for each document, falling back to page_content
            doc_id = doc.metadata.get("id", doc.page_content)
            if doc_id not in fused_scores:
                fused_scores[doc_id] = {"doc": doc, "score": 0.0}
            # Add RRF score
            fused_scores[doc_id]["score"] += 1.0 / (k + rank)

    # Sort documents by their fused scores in descending order
    reranked_results = sorted(fused_scores.values(), key=lambda x: x["score"], reverse=True)

    return [item["doc"] for item in reranked_results]


async def hybrid_search(
    query: str,
    vector_store: ElasticsearchStore,
    category: str | None = None,
    k: int = 2,
) -> List[Document]:
    """
    Performs a hybrid search by manually combining vector and keyword (BM25) searches.
    The results are then re-ranked using Reciprocal Rank Fusion in the application code.
    This approach does not require a paid Elasticsearch license.
    """
    logger.debug(f"Performing manual hybrid search for query: '{query}' with category: '{category}'")

    # 1. Vector Search (KNN) - This is an async operation
    vector_search_kwargs = {'k': k}
    if category:
        vector_search_kwargs["filter"] = [{"term": {"metadata.category": category}}]
    
    vector_results = await vector_store.asimilarity_search(
        query,
        **vector_search_kwargs
    )
    logger.debug(f"Vector search found {len(vector_results)} results.")

    # 2. Keyword Search (BM25) - This is a sync operation
    bm25_query = {
        "query": {
            "bool": {
                "must": [{"match": {"text": {"query": query}}}],
                "filter": [{"term": {"metadata.category": category}}] if category else [],
            }
        },
        "size": k,
    }
    
    # Run the synchronous search call in a separate thread to make it awaitable
    keyword_results_response = await asyncio.to_thread(
        vector_store.client.search,
        index=vector_store._store.index,
        body=bm25_query,
    )
    
    keyword_results = [
        Document(
            page_content=hit["_source"]["text"],
            metadata=hit["_source"]["metadata"]
        )
        for hit in keyword_results_response["hits"]["hits"]
    ]
    logger.debug(f"Keyword search (BM25) found {len(keyword_results)} results.")

    # 3. Re-rank using Reciprocal Rank Fusion
    if not vector_results and not keyword_results:
        return []
        
    reranked_results = reciprocal_rank_fusion([vector_results, keyword_results])
    logger.debug(f"Re-ranked results count: {len(reranked_results)}")

    # Return the top k results after fusion
    return reranked_results[:k]
