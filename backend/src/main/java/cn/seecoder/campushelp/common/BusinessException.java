package cn.seecoder.campushelp.common;

/**
 * Thrown by service-layer code when a business rule is violated.
 * Caught by {@link GlobalExceptionHandler} and translated to a structured ApiResult response.
 * <p>
 * Each subclass or instance carries an HTTP-facing status code so the handler can set the
 * correct response status without inspecting the message string.
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.BAD_REQUEST;
    }

    public int getCode() {
        return code;
    }
}
