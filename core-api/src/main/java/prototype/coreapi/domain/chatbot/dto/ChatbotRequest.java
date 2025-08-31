package prototype.coreapi.domain.chatbot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@Schema(description = "Chatbot query request")
@AllArgsConstructor
public class ChatbotRequest {

    @NotBlank
    @Size(max = 1000, message = "Question length cannot exceed 1000 characters.")
    @Schema(description = "Query text", example = "What is this?")
    private String query;
}