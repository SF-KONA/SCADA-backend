package com.example.demo.domain.controller;

import com.example.demo.domain.common.ApiResponse;
import com.example.demo.domain.service.SuggestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 시연·테스트용 수동 트리거. 실서비스 시에는 ADMIN 권한만 허용해야 함.
 */
@RestController
@RequiredArgsConstructor
public class AdminSuggestionController {

    private final SuggestionService suggestionService;

    @PostMapping("/api/admin/suggestions/generate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generate(
            @RequestParam(required = false) String equipmentId) {

        int created = (equipmentId != null && !equipmentId.isBlank())
                ? suggestionService.generateForEquipment(equipmentId)
                : suggestionService.generateForAll();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("created", created);
        data.put("equipmentId", equipmentId);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }
}
