package prototype.coreapi.global.exception;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 클라이언트 에러
    INVALID_INPUT(400, "파라미터가 유효하지 않습니다."),
    INVALID_INPUT_VALUE(400, "%1이(가) 유효한 값이 아닙니다."),
    PARAMETER_VALIDATION_ERROR(400, "%1"),
    NO_SUCH_CONTENT(400, "존재하지 않는 콘텐츠입니다."),
    NO_SUCH_CONTENT_VALUE(400, "%1이(가) 존재하지 않습니다."),
    ALREADY_EXIST_VALUE(400, "%1이(가) 이미 존재합니다."),
    BAD_REQUEST(400, "잘못된 요청입니다."),

    // 회원 관련 에러
    EXIST_EMAIL(400, "이미 사용중인 이메일입니다."),
    DELETED_USER(400, "이미 탈퇴한 계정입니다."),
    NOT_FOUND_USER(400, "사용자를 찾을 수 없습니다."),

    // 로그인 실패
    LOGIN_FAILED(400, "로그인에 실패했습니다."),
    WRONG_USERNAME(400,  "존재하지 않는 사용자입니다."),
    WRONG_PASSWORD(400,  "잘못된 비밀번호입니다."),

    // 인증, 토큰, 권한 관련 오류
    EXPIRED_TOKEN(401, "로그인이 만료되었습니다."),
    EXPIRED_REFRESH_TOKEN(401,  "리프레시 토큰이 만료되었습니다."),
    INVALID_TOKEN(401, "잘못된 접근입니다."),
    AUTH_FAILED(401, "인증에 실패했습니다."),

    FORBIDDEN(403,  "권한이 없습니다."),
    SUSPENDED_USER(403,  "정지된 계정입니다."),
    INACTIVE_USER(403,  "비활성 계정입니다."),

    // 서버에러
    SERVICE_FAIL(500,  "작업에 실패했습니다."),
    SERVICE_FAIL_VALUE(500,  "%1에 실패했습니다.")
    ;

    private final int code;
    private final String msg;
}
