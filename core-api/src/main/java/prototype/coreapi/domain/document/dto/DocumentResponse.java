package prototype.coreapi.domain.document.dto;

import prototype.coreapi.domain.document.enitity.Document;

import java.time.LocalDateTime;

public record DocumentResponse(
        Long id,
        String name,
        String type,
        long size,
        LocalDateTime createdAt
) {
    public static DocumentResponse from(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getName(),
                document.getType(),
                document.getSize(),
                document.getCreatedAt()
        );
    }
}
