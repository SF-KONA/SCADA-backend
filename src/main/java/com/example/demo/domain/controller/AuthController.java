package com.example.demo.domain.controller;

import com.example.demo.domain.common.ApiResponse;
import com.example.demo.domain.dto.AuthDto;
import com.example.demo.domain.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 0.1 로그인
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDto.LoginResponse>> login(
            @Valid @RequestBody AuthDto.LoginRequest body
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                authService.login(body.getUserId(), body.getPassword())));
    }

    // 0.2 토큰 갱신
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthDto.RefreshResponse>> refresh(
            @Valid @RequestBody AuthDto.RefreshRequest body
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                authService.refresh(body.getRefreshToken())));
    }

    // 0.3 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal String userId
    ) {
        authService.logout(userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // 0.4 이메일 인증코드 발송
    @PostMapping("/email/send")
    public ResponseEntity<ApiResponse<AuthDto.EmailSendResponse>> sendEmailCode(
            @Valid @RequestBody AuthDto.EmailSendRequest body
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                authService.sendEmailCode(body.getEmail(), body.getPurpose())));
    }

    // 0.5 이메일 인증코드 확인
    @PostMapping("/email/verify")
    public ResponseEntity<ApiResponse<AuthDto.EmailVerifyResponse>> verifyEmailCode(
            @Valid @RequestBody AuthDto.EmailVerifyRequest body
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                authService.verifyEmailCode(body.getEmail(), body.getCode(), body.getPurpose())));
    }
}