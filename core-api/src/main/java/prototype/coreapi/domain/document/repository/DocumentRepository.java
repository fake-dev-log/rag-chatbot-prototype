package prototype.coreapi.domain.document.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import prototype.coreapi.domain.document.entity.Document;
import reactor.core.publisher.Flux;

public interface DocumentRepository extends ReactiveCrudRepository<Document, Long> {

    @Query("SELECT DISTINCT category FROM documents WHERE category IS NOT NULL ORDER BY category")
    Flux<String> findDistinctCategories();

    Flux<Document> findAllByOrderByCreatedAtDesc();
}
