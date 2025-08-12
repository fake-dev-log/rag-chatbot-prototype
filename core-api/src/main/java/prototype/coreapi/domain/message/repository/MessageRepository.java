package prototype.coreapi.domain.message.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import prototype.coreapi.domain.message.document.Message;
import reactor.core.publisher.Flux;

@Repository
public interface MessageRepository extends ReactiveMongoRepository<Message, String> {

    Flux<Message> findByChatIdOrderBySequenceAsc(Long chatId);
}
