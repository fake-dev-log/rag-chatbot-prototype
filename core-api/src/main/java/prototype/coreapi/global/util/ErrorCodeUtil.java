package prototype.coreapi.global.util;

import io.micrometer.common.util.StringUtils;
import org.springframework.util.ObjectUtils;

public class ErrorCodeUtil {
    public static String parseMessage(String message, String ...args) {
        if (StringUtils.isBlank(message) || message.isBlank())
            return message;

        if (ObjectUtils.isEmpty(args) || args.length < 1) return message;

        String[] splitMessages = message.split("%");
        if (splitMessages.length < 2)
            return message;

        for (int i = 0; i < args.length; i++) {
            String replaceChar = "%" + (i + 1);
            message = message.replaceFirst(replaceChar, args[i]);
        }
        return message;
    }
}
