package prototype.coreapi.domain.chatbot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Schema(description = "Chatbot query request")
@AllArgsConstructor
@NoArgsConstructor
public class ChatbotRequest {

    @NotBlank
    @Size(max = 1000, message = "Question length cannot exceed 1000 characters.")
    @Schema(description = "Query text", example = "What is this?")
    private String query;

    @Schema(description = "The summary of the conversation so far, to be used as context.", example = "The user asked about RAG chatbots...")
    private String chatHistory;
}