package com.example.demo.domain.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 커스텀 예외
    @ExceptionHandler(ReportException.class)
    public ResponseEntity<ApiResponse<?>> handleReportException(ReportException e) {
        ErrorCode ec = e.getErrorCode();
        return ResponseEntity.status(ec.getHttpStatus())
            .body(ApiResponse.fail(ec.getCode(), e.getMessage()));
    }

    // @Valid 유효성 검사 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .findFirst()
            .orElse(ErrorCode.VALIDATION_ERROR.getMessage());

        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getHttpStatus())
            .body(ApiResponse.fail(ErrorCode.VALIDATION_ERROR.getCode(), message));
    }

    // 그 외 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception e) {
        log.error("Unhandled exception: ", e);
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getHttpStatus())
            .body(ApiResponse.fail(ErrorCode.INTERNAL_ERROR.getCode(),
                                  ErrorCode.INTERNAL_ERROR.getMessage()));
    }
}
