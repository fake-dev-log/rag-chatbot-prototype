package prototype.coreapi.domain.chat.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import prototype.coreapi.domain.chat.dto.ChatProjection;
import prototype.coreapi.domain.chat.entity.Chat;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public interface ChatRepository extends R2dbcRepository<Chat, Long> {

    @Query("""
      SELECT
        c.id,
        m.id AS member_id,
        m.email AS member_email,
        c.title,
        c.last_message_preview,
        c.is_archived,
        c.created_at,
        c.updated_at
      FROM chats c
      JOIN members m ON c.member_id = m.id
      WHERE c.member_id = :memberId
      ORDER BY c.created_at DESC
    """)
    Flux<ChatProjection> findAllByMemberWithEmail(Long memberId);

    @Query("""
      SELECT
        c.id,
        m.id AS member_id,
        m.email AS member_email,
        c.title,
        c.last_message_preview,
        c.is_archived,
        c.created_at,
        c.updated_at
      FROM chats c
      JOIN members m ON c.member_id = m.id
      WHERE c.id = :chatId
    """)
    Mono<ChatProjection> findByIdWithEmail(Long chatId);

    Mono<Long> countByCreatedAtIsBetween(LocalDateTime createdAtAfter, LocalDateTime createdAtBefore);
}
