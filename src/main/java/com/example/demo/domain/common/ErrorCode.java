package com.example.demo.domain.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    BAD_REQUEST                  (HttpStatus.BAD_REQUEST,            "BAD_REQUEST",                  "요청 값을 확인해 주세요."),
    UNAUTHORIZED                 (HttpStatus.UNAUTHORIZED,           "UNAUTHORIZED",                 "아이디 또는 비밀번호가 올바르지 않습니다."),
    FORBIDDEN                    (HttpStatus.FORBIDDEN,              "FORBIDDEN",                    "접근 권한이 없습니다."),
    REPORT_NOT_FOUND             (HttpStatus.NOT_FOUND,              "REPORT_NOT_FOUND",             "해당 리포트를 찾을 수 없습니다."),
    PDF_NOT_FOUND                (HttpStatus.NOT_FOUND,              "PDF_NOT_FOUND",                "해당 PDF를 찾을 수 없습니다."),
    REPORT_NOT_DONE              (HttpStatus.CONFLICT,               "REPORT_NOT_DONE",              "완료되지 않은 리포트입니다."),
    VALIDATION_ERROR             (HttpStatus.UNPROCESSABLE_ENTITY,   "VALIDATION_ERROR",             "입력값을 다시 확인해 주세요."),
    INTERNAL_ERROR               (HttpStatus.INTERNAL_SERVER_ERROR,  "INTERNAL_SERVER_ERROR",        "서버 내부 오류가 발생했습니다. 관리자에게 문의해 주세요."),
    PROCESS_NOT_FOUND            (HttpStatus.NOT_FOUND,              "PROCESS_NOT_FOUND",            "해당 공정을 찾을 수 없습니다."),
    NOT_PARTNER_PROCESS          (HttpStatus.BAD_REQUEST,            "NOT_PARTNER_PROCESS",          "해당 공정이 협력사 공정이 아닙니다."),
    EQUIPMENT_NOT_FOUND          (HttpStatus.NOT_FOUND,              "EQUIPMENT_NOT_FOUND",          "해당 설비를 찾을 수 없습니다."),
    SUGGESTION_NOT_FOUND         (HttpStatus.NOT_FOUND,              "SUGGESTION_NOT_FOUND",         "해당 AI 제안을 찾을 수 없습니다."),
    PARAMETER_NOT_FOUND          (HttpStatus.NOT_FOUND,              "PARAMETER_NOT_FOUND",          "해당 파라미터를 찾을 수 없습니다."),
    NOT_CONTROLLABLE             (HttpStatus.FORBIDDEN,              "NOT_CONTROLLABLE",             "제어가 불가능한 파라미터입니다."),
    INVALID_STATUS_TRANSITION    (HttpStatus.CONFLICT,               "INVALID_STATUS_TRANSITION",    "허용되지 않는 상태 전환입니다."),
    ALARM_NOT_FOUND              (HttpStatus.NOT_FOUND,              "ALARM_NOT_FOUND",              "해당 알람을 찾을 수 없습니다."),
    USER_NOT_FOUND               (HttpStatus.NOT_FOUND,              "USER_NOT_FOUND",               "해당 사용자를 찾을 수 없습니다."),
    DUPLICATE_USER_ID            (HttpStatus.CONFLICT,               "DUPLICATE_USER_ID",            "이미 사용 중인 아이디입니다."),
    DUPLICATE_EMAIL              (HttpStatus.CONFLICT,               "DUPLICATE_EMAIL",              "이미 사용 중인 이메일입니다."),
    EMAIL_NOT_FOUND              (HttpStatus.NOT_FOUND,              "EMAIL_NOT_FOUND",              "해당 이메일로 가입된 계정을 찾을 수 없습니다."),
    EMAIL_CODE_NOT_FOUND         (HttpStatus.BAD_REQUEST,            "EMAIL_CODE_NOT_FOUND",         "인증코드 발송 이력이 없습니다. 다시 요청해 주세요."),
    EMAIL_CODE_INVALID           (HttpStatus.BAD_REQUEST,            "EMAIL_CODE_INVALID",           "인증코드가 올바르지 않거나 만료되었습니다."),
    TOKEN_EXPIRED                (HttpStatus.UNAUTHORIZED,           "TOKEN_EXPIRED",                "토큰이 만료되었습니다. 다시 로그인해 주세요."),
    TOKEN_INVALID                (HttpStatus.UNAUTHORIZED,           "TOKEN_INVALID",                "유효하지 않은 토큰입니다."),
    ACCOUNT_LOCKED               (HttpStatus.FORBIDDEN,              "ACCOUNT_LOCKED",               "잠금 처리된 계정입니다. 관리자에게 문의해 주세요."),
    ACCOUNT_INACTIVE             (HttpStatus.FORBIDDEN,              "ACCOUNT_INACTIVE",             "비활성화된 계정입니다. 관리자에게 문의해 주세요.");

    private final HttpStatus httpStatus;
    private final String     code;
    private final String     message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code       = code;
        this.message    = message;
    }
}