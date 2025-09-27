package prototype.coreapi.domain.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TitleGenerationRequest {
    private String question;
    private String answer;
}
