package prototype.coreapi.global.mongosequence;

import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class SequenceGeneratorService {

    private final ReactiveMongoOperations mongoOps;

    public Mono<Long> generateSequence(String seqName) {
        return mongoOps.findAndModify(
                        Query.query(Criteria.where("_id").is(seqName)),
                        new Update().inc("seq",1),
                        FindAndModifyOptions.options().returnNew(true).upsert(true),
                        DatabaseSequence.class
                )
                .map(DatabaseSequence::getSeq)
                .defaultIfEmpty(1L);
    }
}