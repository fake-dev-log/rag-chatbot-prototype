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
@Schema(description = "사용자 생성/수정 요청")
public class MemberRequest {

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @Schema(description = "이메일", example = "test@example.com")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Schema(description = "비밀번호", example = "P@ssw0rd!")
    private String password;

    @Schema(description = "회원 역할", example = "USER")
    private Role role;

    @Schema(description = "회원 상태", example = "ACTIVE")
    private Status status;
}
