package com.example.demo.domain.controller;

import com.example.demo.domain.common.ApiResponse;
import com.example.demo.domain.dto.SuggestionDto;
import com.example.demo.domain.service.ParameterControlService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ParameterControlController {

    private final ParameterControlService parameterControlService;

    // 5.5 제어 가능 파라미터 목록
    @GetMapping("/api/equipments/{equipmentId}/parameters/controllable")
    public ResponseEntity<ApiResponse<SuggestionDto.ControllableListResponse>> listControllable(
            @PathVariable String equipmentId) {
        return ResponseEntity.ok(ApiResponse.ok(
                parameterControlService.listControllable(equipmentId)));
    }

    // 5.6 파라미터 수동 변경
    @PatchMapping("/api/equipments/{equipmentId}/parameters/{paramId}")
    public ResponseEntity<ApiResponse<SuggestionDto.ParamUpdateResponse>> updateParameter(
            @PathVariable String equipmentId,
            @PathVariable Long paramId,
            @RequestBody SuggestionDto.ParamUpdateRequest request) {
        // TODO: JWT 구현 후 실제 userId 사용
        String userId = "dev_user";
        return ResponseEntity.ok(ApiResponse.ok(
                parameterControlService.updateParameter(
                        equipmentId, paramId,
                        request.getNewValue(), request.getComment(),
                        userId)));
    }
}
