package prototype.coreapi.domain.document.dto;

import prototype.coreapi.domain.document.enums.IndexingStatus;

public record StatusUpdateRequest(
    IndexingStatus status
) {}
