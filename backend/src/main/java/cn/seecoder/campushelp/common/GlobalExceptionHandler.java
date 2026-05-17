package cn.seecoder.campushelp.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResult<Void>> handleBusiness(BusinessException e) {
        HttpStatus httpStatus = switch (e.getCode()) {
            case ResultCode.FORBIDDEN   -> HttpStatus.FORBIDDEN;
            case ResultCode.NOT_FOUND   -> HttpStatus.NOT_FOUND;
            case ResultCode.CONFLICT    -> HttpStatus.CONFLICT;
            default                     -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(httpStatus)
                .body(ApiResult.error(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<Void>> handleValidation(MethodArgumentNotValidException e) {
        String fields = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getField)
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
                .body(ApiResult.error(ResultCode.BAD_REQUEST, "参数校验失败: " + fields));
    }

    // Catch-all for unexpected errors — log the stack but don't leak it to the client
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleGeneral(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.error(ResultCode.INTERNAL_ERROR, "服务器内部错误"));
    }
}
