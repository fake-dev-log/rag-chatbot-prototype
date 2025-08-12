package prototype.coreapi.domain.chatbot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SourceDocumentProjection(
        @JsonProperty("file_name")   String fileName,
        @JsonProperty("title")       String title,
        @JsonProperty("page_number") int pageNumber,
        @JsonProperty("snippet")     String snippet
) {}
