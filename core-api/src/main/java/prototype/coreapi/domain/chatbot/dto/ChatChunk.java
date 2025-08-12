package prototype.coreapi.domain.chatbot.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record ChatChunk(
        String type,
        JsonNode data
) {}