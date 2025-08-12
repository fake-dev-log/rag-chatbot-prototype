package prototype.coreapi.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ServerWebInputException;
import prototype.coreapi.global.response.ErrorResponse;
import prototype.coreapi.global.response.RestResponse;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleBusinessException(BusinessException e) {
        log.error("BusinessException ::: {}", e.getMessage());
        return Mono.just(RestResponse.customError(e));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleWebExchangeBind(WebExchangeBindException e) {
        log.error("Validation failed: {}", e.getMessage());
        String msg = e.getFieldErrors().stream()
                .map(fe -> fe.getField()
                        + ": " + fe.getDefaultMessage()
                        + " 입력된 값: [" + fe.getRejectedValue() + "]")
                .collect(Collectors.joining(", "));
        return Mono.just(
                RestResponse.customError(ErrorCode.PARAMETER_VALIDATION_ERROR, msg)
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.error("TypeMismatch ::: {}", e.getMessage());
        String rejected = e.getValue() == null ? "" : e.getValue().toString();
        return Mono.just(
                RestResponse.customError(ErrorCode.PARAMETER_VALIDATION_ERROR, rejected)
        );
    }

    @ExceptionHandler(ServerWebInputException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleWebInput(ServerWebInputException e) {
        log.error("Bad request ::: {}", e.getMessage());
        return Mono.just(
                RestResponse.customError(ErrorCode.BAD_REQUEST)
        );
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleException(Exception e) {
        log.error("Unhandled exception ::: {}", e.getMessage(), e);
        return Mono.just(
                RestResponse.customError(ErrorCode.SERVICE_FAIL)
        );
    }
}