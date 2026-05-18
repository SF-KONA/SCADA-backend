package com.example.demo.domain.dto;

import com.example.demo.domain.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public class UserDto {

    // ── 목록 조회 응답 ───────────────────────────────────────────────────────
    @Getter
    @AllArgsConstructor
    public static class ListResponse {
        private final List<UserItem> items;
        private final int total;
    }

    @Getter
    @AllArgsConstructor
    public static class UserItem {
        private final String userId;
        private final String name;
        private final String email;
        private final String factoryCode;
        private final String factory;
        private final String role;
        private final String roleLabel;
        private final String status;
        private final OffsetDateTime lastLoginAt;
        private final OffsetDateTime createdAt;
    }

    // ── 상세 조회 응답 (lines 포함) ──────────────────────────────────────────
    @Getter
    @AllArgsConstructor
    public static class UserDetailResponse {
        private final String userId;
        private final String name;
        private final String email;
        private final String factoryCode;
        private final String factory;
        private final String role;
        private final String roleLabel;
        private final String status;
        private final OffsetDateTime lastLoginAt;
        private final OffsetDateTime createdAt;
        private final List<LineItem> lines;
    }

    // ── 담당 라인 ────────────────────────────────────────────────────────────
    @Getter
    @AllArgsConstructor
    public static class LineItem {
        private final String lineCode;
        private final OffsetDateTime assignedAt;
    }

    @Getter
    @AllArgsConstructor
    public static class LinesResponse {
        private final String userId;
        private final List<LineItem> lines;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateLinesRequest {
        @NotNull
        private List<String> lineCodes;
    }

    // ── 변경 이력 ────────────────────────────────────────────────────────────
    @Getter
    @AllArgsConstructor
    public static class AuditResponse {
        private final String userId;
        private final int totalCount;
        private final int page;
        private final int size;
        private final List<AuditLogItem> logs;
    }

    @Getter
    @AllArgsConstructor
    public static class AuditLogItem {
        private final Long logId;
        private final String action;
        private final String actionLabel;
        private final String performedBy;
        private final Object detail;
        private final OffsetDateTime occurredAt;
    }

    // ── 등록 요청/응답 ───────────────────────────────────────────────────────
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9]{4,32}$", message = "아이디는 영문·숫자 4~32자로 입력해주세요.")
        private String userId;

        @NotBlank
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",
                 message = "비밀번호는 8자 이상, 영문·숫자·특수문자를 포함해야 합니다.")
        private String password;

        @NotBlank
        private String name;

        @NotBlank @Email
        private String email;

        private String factoryCode;

        @NotNull
        private UserRole role;
    }

    @Getter
    @AllArgsConstructor
    public static class RegisterResponse {
        private final String userId;
        private final String role;
    }

    // ── 수정 요청 (PATCH — 모든 필드 선택적) ────────────────────────────────
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private String name;
        @Email
        private String email;
        private String factoryCode;
        private UserRole role;
    }

    // ── 비밀번호 초기화 응답 ─────────────────────────────────────────────────
    @Getter
    @AllArgsConstructor
    public static class ResetPasswordResponse {
        private final String userId;
        private final String temporaryPassword;
        private final OffsetDateTime resetAt;
    }

    // ── 아이디 중복 확인 응답 ────────────────────────────────────────────────
    @Getter
    @AllArgsConstructor
    public static class CheckIdResponse {
        private final String userId;
        private final boolean isDuplicate;
    }

    // ── 이메일 중복 확인 응답 ────────────────────────────────────────────────
    @Getter
    @AllArgsConstructor
    public static class CheckEmailResponse {
        private final String email;
        private final boolean isDuplicate;
    }

    // ── 공장 목록 ────────────────────────────────────────────────────────────
    @Getter
    @AllArgsConstructor
    public static class FactoryItem {
        private final String factoryCode;
        private final String factoryName;
    }
}
