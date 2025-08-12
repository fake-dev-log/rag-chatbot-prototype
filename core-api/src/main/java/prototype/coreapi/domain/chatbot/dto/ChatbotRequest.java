package prototype.coreapi.domain.chatbot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@Schema(description = "봇에 대한 질의 요청")
@AllArgsConstructor
public class ChatbotRequest {

    @NotBlank
    @Size(max = 1000, message = "질문 길이는 최대 1000자 입니다.")
    @Schema(description = "질의문", example = "이게 뭐야?")
    private String query;
}