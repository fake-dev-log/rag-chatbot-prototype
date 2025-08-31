package prototype.coreapi.domain.prompt.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import prototype.coreapi.domain.prompt.entity.PromptTemplate;
import reactor.core.publisher.Mono;

/**
 * Spring Data R2DBC repository for the {@link PromptTemplate} entity.
 */
@Repository
public interface PromptTemplateRepository extends ReactiveCrudRepository<PromptTemplate, Long> {
    Mono<PromptTemplate> findByName(String name);
}