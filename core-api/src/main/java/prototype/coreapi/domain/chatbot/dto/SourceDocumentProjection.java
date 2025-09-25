package prototype.coreapi.domain.chatbot.dto;

public record SourceDocumentProjection(
        String fileName,
        String title,
        int pageNumber,
        String snippet
) {}
