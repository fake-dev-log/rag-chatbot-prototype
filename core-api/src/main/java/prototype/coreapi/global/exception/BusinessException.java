package prototype.coreapi.global.exception;

import prototype.coreapi.global.util.ErrorCodeUtil;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMsg());
        this.errorCode = errorCode;
    }

    public BusinessException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String... args) {
        super(ErrorCodeUtil.parseMessage(errorCode.getMsg(), args));
        this.errorCode = errorCode;
    }
}
