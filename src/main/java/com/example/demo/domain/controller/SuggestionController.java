package com.example.demo.domain.controller;

import com.example.demo.domain.common.ApiResponse;
import com.example.demo.domain.dto.SuggestionDto;
import com.example.demo.domain.service.SuggestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class SuggestionController {

    private final SuggestionService suggestionService;

    // 5.1 AI 제안 목록
    @GetMapping("/api/suggestions")
    public ResponseEntity<ApiResponse<SuggestionDto.ListResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String equipmentId) {
        size = Math.min(size, 100);
        return ResponseEntity.ok(ApiResponse.ok(suggestionService.list(page, size, equipmentId)));
    }

    // 5.2 AI 제안 상세
    @GetMapping("/api/suggestions/{suggestionId}")
    public ResponseEntity<ApiResponse<SuggestionDto.DetailResponse>> get(
            @PathVariable Long suggestionId) {
        return ResponseEntity.ok(ApiResponse.ok(suggestionService.get(suggestionId)));
    }

    // 5.3 AI 제안 적용
    @PostMapping("/api/suggestions/{suggestionId}/apply")
    public ResponseEntity<ApiResponse<SuggestionDto.ActionResponse>> apply(
            @PathVariable Long suggestionId,
            @RequestBody(required = false) SuggestionDto.ActionRequest request) {
        // TODO: JWT 구현 후 실제 userId 사용
        String userId = "dev_user";
        String comment = request != null ? request.getComment() : null;
        return ResponseEntity.ok(ApiResponse.ok(
                suggestionService.apply(suggestionId, userId, comment)));
    }

    // 5.4 AI 제안 거부
    @PostMapping("/api/suggestions/{suggestionId}/reject")
    public ResponseEntity<ApiResponse<SuggestionDto.ActionResponse>> reject(
            @PathVariable Long suggestionId,
            @RequestBody(required = false) SuggestionDto.ActionRequest request) {
        // TODO: JWT 구현 후 실제 userId 사용
        String userId = "dev_user";
        String comment = request != null ? request.getComment() : null;
        return ResponseEntity.ok(ApiResponse.ok(
                suggestionService.reject(suggestionId, userId, comment)));
    }
}
