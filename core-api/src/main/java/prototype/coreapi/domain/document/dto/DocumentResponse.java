package prototype.coreapi.domain.document.dto;

import prototype.coreapi.domain.document.entity.Document;
import prototype.coreapi.domain.document.enums.IndexingStatus;

import java.time.LocalDateTime;

public record DocumentResponse(
        Long id,
        String name,
        String type,
        long size,
        String category,
        IndexingStatus status,
        LocalDateTime createdAt
) {
    public static DocumentResponse from(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getName(),
                document.getType(),
                document.getSize(),
                document.getCategory(),
                document.getStatus(),
                document.getCreatedAt()
        );
    }
}
