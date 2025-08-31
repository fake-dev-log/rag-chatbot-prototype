package prototype.coreapi.global.exception;


import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Enum representing various error codes and their corresponding messages.
 * These error codes are used throughout the application to provide standardized
 * and localized error responses for different scenarios.
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    // Client Error
    INVALID_INPUT(400, "Invalid parameter."),
    INVALID_INPUT_VALUE(400, "%1 is not a valid value."),
    PARAMETER_VALIDATION_ERROR(400, "%1"),
    NO_SUCH_CONTENT(400, "Content does not exist."),
    NO_SUCH_CONTENT_VALUE(400, "%1 does not exist."),
    ALREADY_EXIST_VALUE(400, "%1 already exists."),
    BAD_REQUEST(400, "Bad request."),

    // Member related errors
    EXIST_EMAIL(400, "This email is already in use."),
    DELETED_USER(400, "This account has already been deleted."),
    NOT_FOUND_USER(400, "User not found."),

    // Sign-in failed
    SIGN_IN_FAILED(400, "Sign-in failed."),
    WRONG_USERNAME(400,  "User does not exist."),
    WRONG_PASSWORD(400,  "Incorrect password."),

    // Authentication, token, and permission related errors
    EXPIRED_TOKEN(401, "Sign-in has expired."),
    EXPIRED_REFRESH_TOKEN(401,  "Refresh token has expired."),
    INVALID_TOKEN(401, "Invalid access."),
    AUTH_FAILED(401, "Authentication failed."),

    FORBIDDEN(403,  "You do not have permission."),
    SUSPENDED_USER(403,  "This account is suspended."),
    INACTIVE_USER(403,  "This account is inactive."),

    // Server Error
    SERVICE_FAIL(500,  "The operation failed."),
    SERVICE_FAIL_VALUE(500,  "Failed to %1.")
    ;

    private final int code;
    private final String msg;
}