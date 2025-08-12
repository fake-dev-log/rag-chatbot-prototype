package prototype.coreapi.global.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Schema(description = "회원 권한")
public enum Role {
    ADMIN,
    MANAGER,
    USER
}