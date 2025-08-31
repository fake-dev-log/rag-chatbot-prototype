package prototype.coreapi.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import prototype.coreapi.global.enums.Role;
import prototype.coreapi.global.enums.Status;

@Getter
@Setter
@Schema(description = "User create/update request")
public class MemberRequest {

    @NotBlank(message = "Email is required.")
    @Email(message = "The email format is incorrect.")
    @Schema(description = "Email", example = "test@example.com")
    private String email;

    @NotBlank(message = "Password is required.")
    @Schema(description = "Password", example = "P@ssw0rd!")
    private String password;

    @Schema(description = "Member role", example = "USER")
    private Role role;

    @Schema(description = "Member status", example = "ACTIVE")
    private Status status;
}