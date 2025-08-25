package prototype.coreapi.domain.document.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import prototype.coreapi.domain.document.Document;

public interface DocumentRepository extends ReactiveCrudRepository<Document, Long> {
}
