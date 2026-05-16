package com.example.demo.domain.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    BAD_REQUEST         (HttpStatus.BAD_REQUEST,            "BAD_REQUEST",           "요청 파라미터 형식 오류"),
    UNAUTHORIZED        (HttpStatus.UNAUTHORIZED,           "UNAUTHORIZED",          "JWT 토큰 없음 또는 만료"),
    FORBIDDEN           (HttpStatus.FORBIDDEN,              "FORBIDDEN",             "권한 없음"),
    REPORT_NOT_FOUND    (HttpStatus.NOT_FOUND,              "REPORT_NOT_FOUND",      "리포트 없음"),
    PDF_NOT_FOUND       (HttpStatus.NOT_FOUND,              "PDF_NOT_FOUND",         "PDF 없음"),
    REPORT_NOT_DONE     (HttpStatus.CONFLICT,               "REPORT_NOT_DONE",       "완료되지 않은 리포트"),
    VALIDATION_ERROR    (HttpStatus.UNPROCESSABLE_ENTITY,   "VALIDATION_ERROR",      "입력값 유효성 오류"),
    INTERNAL_ERROR      (HttpStatus.INTERNAL_SERVER_ERROR,  "INTERNAL_SERVER_ERROR", "서버 내부 오류"),
    PROCESS_NOT_FOUND  (HttpStatus.NOT_FOUND,   "PROCESS_NOT_FOUND",   "해당 공정을 찾을 수 없습니다."),
    NOT_PARTNER_PROCESS(HttpStatus.BAD_REQUEST,  "NOT_PARTNER_PROCESS", "해당 공정이 협력사 공정이 아닙니다."),
    EQUIPMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "EQUIPMENT_NOT_FOUND", "해당 설비를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String     code;
    private final String     message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code       = code;
        this.message    = message;
    }
}
