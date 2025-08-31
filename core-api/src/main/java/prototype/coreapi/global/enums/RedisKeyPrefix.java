package prototype.coreapi.global.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Schema(description = "Redis search key prefix")
public enum RedisKeyPrefix {

    REFRESH_TOKEN("RT"),
    ACCESS_TOKEN_HASH("AT_HASH"),
    ACCESS_TOKEN_BLACKLIST("BL_HASH");

    private final String prefix;

    public String key(Object... parts) {
        StringBuilder sb = new StringBuilder(prefix);
        for (Object part : parts) {
            sb.append(":").append(part);
        }
        return sb.toString();
    }
}