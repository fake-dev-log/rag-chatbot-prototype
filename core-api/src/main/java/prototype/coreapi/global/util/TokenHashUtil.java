package prototype.coreapi.global.util;

import org.apache.commons.codec.digest.DigestUtils;

public class TokenHashUtil {
    public static String sha256(String token) {
        return DigestUtils.sha256Hex(token);
    }
}
