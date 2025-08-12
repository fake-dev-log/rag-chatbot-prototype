package prototype.coreapi.domain.chat.dto;

import java.time.LocalDateTime;

public record ChatProjection (
        Long id,
        Long memberId,
        String memberEmail,
        String title,
        String lastMessagePreview,
        Boolean isArchived,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
