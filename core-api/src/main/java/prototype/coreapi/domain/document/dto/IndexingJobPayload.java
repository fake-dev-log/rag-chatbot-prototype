package prototype.coreapi.domain.document.dto;

import lombok.Builder;

@Builder
public record IndexingJobPayload(
        Long documentId,
        String storedName,
        String category
) {
}
