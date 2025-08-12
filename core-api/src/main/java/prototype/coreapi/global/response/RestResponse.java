package prototype.coreapi.global.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import prototype.coreapi.global.exception.BusinessException;
import prototype.coreapi.global.exception.ErrorCode;
import prototype.coreapi.global.util.ErrorCodeUtil;

import java.util.Map;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class RestResponse<T> {
    private int code;
    private T data;

    public static <T> ResponseEntity<T> ok() {
        return RestResponse.<T>builder()
                .code(HttpStatus.OK.value())
                .build()
                .toResponseEntity();
    }

    public static <T> ResponseEntity<T> ok(T data) {
        return RestResponse.<T>builder()
                .code(HttpStatus.OK.value())
                .data(data)
                .build()
                .toResponseEntity();
    }

    public static <T> ResponseEntity<T> ok(Map<String, String> headers, T data) {
        return RestResponse.<T>builder()
                .code(HttpStatus.OK.value())
                .data(data)
                .build()
                .toResponseEntity(headers);
    }

    public static <T> ResponseEntity<T> created(T data) {
        return RestResponse.<T>builder()
                .code(HttpStatus.CREATED.value())
                .data(data)
                .build()
                .toResponseEntity();
    }

    public static <T> ResponseEntity<T> created() {
        return RestResponse.<T>builder()
                .code(HttpStatus.CREATED.value())
                .build().toResponseEntity();
    }

    public static <T> ResponseEntity<T> deleted() {
        return RestResponse.<T>builder()
                .code(HttpStatus.NO_CONTENT.value())
                .build().toResponseEntity();
    }

    public static ResponseEntity<ErrorResponse> customError(BusinessException e) {
        final ErrorCode errorCode = e.getErrorCode();
        if (errorCode == null) {
            final ErrorResponse errorResponse = new ErrorResponse(ErrorCode.SERVICE_FAIL);
            return RestResponse.<ErrorResponse>builder()
                    .code(ErrorCode.SERVICE_FAIL.getCode())
                    .data(errorResponse)
                    .build()
                    .toResponseEntity();
        }
        final ErrorResponse errorResponse = new ErrorResponse(errorCode, e.getMessage());
        return RestResponse.<ErrorResponse>builder()
                .code(errorCode.getCode())
                .data(errorResponse)
                .build()
                .toResponseEntity();
    }

    public static ResponseEntity<ErrorResponse> customError(ErrorCode e, String... args) {
        final ErrorResponse errorResponse = new ErrorResponse(e, ErrorCodeUtil.parseMessage(e.getMsg(), args));
        return RestResponse.<ErrorResponse>builder()
                .code(e.getCode())
                .data(errorResponse)
                .build()
                .toResponseEntity();
    }

    public ResponseEntity<T> toResponseEntity() {
        return new ResponseEntity<>(
                this.data,
                generateHeaders(),
                HttpStatus.valueOf(this.code));
    }

    public ResponseEntity<T> toResponseEntity(Map<String, String> headers) {
        HttpHeaders httpHeaders = generateHeaders();
        headers.forEach(httpHeaders::set);

        return new ResponseEntity<>(
                this.data,
                httpHeaders,
                HttpStatus.valueOf(this.code));
    }

    private HttpHeaders generateHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Accept-Charset", "utf-8");
        return headers;
    }
}
