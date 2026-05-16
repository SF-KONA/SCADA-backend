package com.example.demo.domain.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@Getter
public class ApiResponse<T> {

    private final boolean success;
    private final int code;
    private final String message;
    private final T data;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final ErrorBody error;

    private ApiResponse(boolean success, int code, String message, T data, ErrorBody error) {
        this.success = success;
        this.code    = code;
        this.message = message;
        this.data    = data;
        this.error   = error;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, 200, "요청에 성공하였습니다.", data, null);
    }

    public static <T> ApiResponse<T> fail(String code, String message) {
        return new ApiResponse<>(false, 400, message, null, new ErrorBody(code, message));
    }

    public static <T> ApiResponse<T> fail(String code, String message, int httpStatus) {
        return new ApiResponse<>(false, httpStatus, message, null, new ErrorBody(code, message));
    }

    @Getter
    public static class ErrorBody {
        private final String code;
        private final String detail;

        public ErrorBody(String code, String detail) {
            this.code   = code;
            this.detail = detail;
        }
    }
}