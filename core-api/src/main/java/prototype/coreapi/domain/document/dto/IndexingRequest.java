package prototype.coreapi.domain.document.dto;

public record IndexingRequest(
        String file_path,
        String document_name
) {
}
