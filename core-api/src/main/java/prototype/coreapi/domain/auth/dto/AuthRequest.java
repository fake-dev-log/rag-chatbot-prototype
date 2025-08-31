package prototype.coreapi.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
@Schema(description = "Sign-in request")
public class AuthRequest {

    @NotBlank(message = "Email is required.")
    @Email(message = "The email format is incorrect.")
    @Schema(description = "email", example = "test@example.com")
    private String email;

    @NotBlank(message = "Password is required.")
    @Schema(description = "Password", example = "password123")
    private String password;
}
