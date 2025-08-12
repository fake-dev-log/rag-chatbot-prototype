package prototype.coreapi.domain.member.repository;

import prototype.coreapi.domain.member.entity.Member;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface MemberRepository extends ReactiveCrudRepository<Member, Long> {

    Mono<Boolean> existsByEmail(String email);

    Mono<Member> findByEmail(String email);
}