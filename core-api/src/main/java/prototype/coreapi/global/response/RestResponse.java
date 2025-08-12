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

    public static ResponseEntity<String> customError(BusinessException e) {
        RestResponse<String> response;
        if (e.getErrorCode() == null) {
            response = RestResponse.<String>builder()
                    .code(ErrorCode.SERVICE_FAIL.getCode())
                    .data(ErrorCode.SERVICE_FAIL.getMsg())
                    .build();
        } else {
            response = RestResponse.<String>builder()
                    .code(e.getErrorCode().getCode())
                    .data(e.getMessage())
                    .build();
        }
        return response.toResponseEntity();
    }

    public static ResponseEntity<String> customError(ErrorCode e, String... args) {
        return RestResponse.<String>builder()
                .code(e.getCode())
                .data(ErrorCodeUtil.parseMessage(e.getMsg(), args))
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
