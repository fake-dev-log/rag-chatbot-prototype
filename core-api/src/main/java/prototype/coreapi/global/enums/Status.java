package prototype.coreapi.global.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Schema(description = "회원 활성 상태")
public enum Status {

    ACTIVE,
    INACTIVE,
    SUSPENDED,
    DELETED
}
