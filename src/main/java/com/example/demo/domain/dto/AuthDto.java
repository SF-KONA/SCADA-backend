package com.example.demo.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Getter;

public class AuthDto {

    // ── 로그인 요청 ───────────────────────────────
    @Getter
    public static class LoginRequest {
        @NotBlank
        private String userId;
        @NotBlank
        private String password;
    }

    // ── 로그인 응답 ───────────────────────────────
    @Getter
    @Builder
    public static class LoginResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType;
        private int accessTokenExpiresIn;
        private String userId;
        private String name;
        private String role;
        private String roleLabel;
    }

    // ── 토큰 갱신 요청 ────────────────────────────
    @Getter
    public static class RefreshRequest {
        @NotBlank
        private String refreshToken;
    }

    // ── 토큰 갱신 응답 ────────────────────────────
    @Getter
    @Builder
    public static class RefreshResponse {
        private String accessToken;
        private String tokenType;
        private int accessTokenExpiresIn;
    }

    // ── 이메일 인증코드 발송 요청 (0.4) ──────────
    @Getter
    public static class EmailSendRequest {
        @NotBlank
        @Email
        private String email;

        @NotBlank
        @Pattern(regexp = "FIND_ID|FIND_PW", message = "purpose는 FIND_ID 또는 FIND_PW 이어야 합니다.")
        private String purpose;
    }

    // ── 이메일 인증코드 발송 응답 (0.4) ──────────
    @Getter
    @Builder
    public static class EmailSendResponse {
        private String email;
        private int expiresIn;  // 고정 180초
    }

    // ── 이메일 인증코드 확인 요청 (0.5) ──────────
    @Getter
    public static class EmailVerifyRequest {
        @NotBlank
        @Email
        private String email;

        @NotBlank
        private String code;

        @NotBlank
        @Pattern(regexp = "FIND_ID|FIND_PW", message = "purpose는 FIND_ID 또는 FIND_PW 이어야 합니다.")
        private String purpose;
    }

    // ── 이메일 인증코드 확인 응답 (0.5) ──────────
    @Getter
    @Builder
    public static class EmailVerifyResponse {
        private String userId;
        private String temporaryPassword;  // FIND_PW일 때만
        private String resetAt;            // FIND_PW일 때만
    }
}