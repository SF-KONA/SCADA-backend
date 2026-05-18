package com.example.demo.domain.controller;

import com.example.demo.domain.common.ApiResponse;
import com.example.demo.domain.dto.UserDto;
import com.example.demo.domain.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ── 목록 조회 ────────────────────────────────────────────────────────────
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<UserDto.ListResponse>> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getUsers(keyword, null, role, status)));
    }

    // ── 상세 조회 ────────────────────────────────────────────────────────────
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<UserDto.UserDetailResponse>> getUser(
            @PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getUserDetail(userId)));
    }

    // ── 등록 ─────────────────────────────────────────────────────────────────
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users")
    public ResponseEntity<ApiResponse<UserDto.RegisterResponse>> createUser(
            @Valid @RequestBody UserDto.CreateRequest req,
            @AuthenticationPrincipal String performedBy) {
        return ResponseEntity.ok(ApiResponse.ok(userService.createUser(req, performedBy)));
    }

    // ── 정보 수정 (PATCH) ────────────────────────────────────────────────────
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<UserDto.UserItem>> updateUser(
            @PathVariable String userId,
            @RequestBody UserDto.UpdateRequest req,
            @AuthenticationPrincipal String performedBy) {
        return ResponseEntity.ok(ApiResponse.ok(userService.updateUser(userId, req, performedBy)));
    }

    // ── 비밀번호 초기화 ──────────────────────────────────────────────────────
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users/{userId}/password/reset")
    public ResponseEntity<ApiResponse<UserDto.ResetPasswordResponse>> resetPassword(
            @PathVariable String userId,
            @AuthenticationPrincipal String performedBy) {
        return ResponseEntity.ok(ApiResponse.ok(userService.resetPassword(userId, performedBy)));
    }

    // ── 계정 잠금 ────────────────────────────────────────────────────────────
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users/{userId}/lock")
    public ResponseEntity<ApiResponse<Void>> lockUser(@PathVariable String userId) {
        userService.lockUser(userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ── 잠금 해제 ────────────────────────────────────────────────────────────
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users/{userId}/unlock")
    public ResponseEntity<ApiResponse<Void>> unlockUser(@PathVariable String userId) {
        userService.unlockUser(userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ── 계정 비활성화 ────────────────────────────────────────────────────────
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users/{userId}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(@PathVariable String userId) {
        userService.deactivateUser(userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ── 계정 활성화 ──────────────────────────────────────────────────────────
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users/{userId}/activate")
    public ResponseEntity<ApiResponse<Void>> activateUser(@PathVariable String userId) {
        userService.activateUser(userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ── 담당 라인 조회 ───────────────────────────────────────────────────────
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/{userId}/lines")
    public ResponseEntity<ApiResponse<UserDto.LinesResponse>> getUserLines(
            @PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getUserLines(userId)));
    }

    // ── 담당 라인 수정 ───────────────────────────────────────────────────────
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/users/{userId}/lines")
    public ResponseEntity<ApiResponse<UserDto.LinesResponse>> updateUserLines(
            @PathVariable String userId,
            @Valid @RequestBody UserDto.UpdateLinesRequest req,
            @AuthenticationPrincipal String performedBy) {
        return ResponseEntity.ok(ApiResponse.ok(
                userService.updateUserLines(userId, req.getLineCodes(), performedBy)));
    }

    // ── 변경 이력 조회 ───────────────────────────────────────────────────────
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/{userId}/audit")
    public ResponseEntity<ApiResponse<UserDto.AuditResponse>> getAuditLogs(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getAuditLogs(userId, page, size)));
    }

    // ── 아이디 중복 확인 ─────────────────────────────────────────────────────
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/check-id")
    public ResponseEntity<ApiResponse<UserDto.CheckIdResponse>> checkUserId(
            @RequestParam String userId) {
        return ResponseEntity.ok(ApiResponse.ok(
                new UserDto.CheckIdResponse(userId, !userService.checkUserIdAvailable(userId))));
    }

    // ── 이메일 중복 확인 ─────────────────────────────────────────────────────
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/check-email")
    public ResponseEntity<ApiResponse<UserDto.CheckEmailResponse>> checkEmail(
            @RequestParam String email,
            @RequestParam(required = false) String excludeUserId) {
        return ResponseEntity.ok(ApiResponse.ok(
                new UserDto.CheckEmailResponse(email, !userService.checkEmailAvailable(email, excludeUserId))));
    }

    // ── 공장 목록 ────────────────────────────────────────────────────────────
    @GetMapping("/factories")
    public ResponseEntity<ApiResponse<List<UserDto.FactoryItem>>> getFactories() {
        return ResponseEntity.ok(ApiResponse.ok(userService.getFactories()));
    }
}
