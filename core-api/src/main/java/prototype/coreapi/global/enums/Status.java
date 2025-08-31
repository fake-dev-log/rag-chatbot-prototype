package prototype.coreapi.global.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Schema(description = "Member activation status")
public enum Status {

    ACTIVE,
    INACTIVE,
    SUSPENDED,
    DELETED
}
