package com.example.demo.domain.common;

import lombok.Getter;

@Getter
public class ReportException extends RuntimeException {

    private final ErrorCode errorCode;

    public ReportException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ReportException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
