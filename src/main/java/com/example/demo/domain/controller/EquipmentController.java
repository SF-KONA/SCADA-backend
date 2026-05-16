package com.example.demo.domain.controller;

import com.example.demo.domain.common.ApiResponse;
import com.example.demo.domain.dto.EquipmentDto;
import com.example.demo.domain.service.EquipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class EquipmentController {

    private final EquipmentService equipmentService;

    // 4.1 공정별 설비 목록
    @GetMapping("/api/processes/{stepNo}/equipments")
    public ResponseEntity<ApiResponse<EquipmentDto.EquipmentListResponse>> getEquipmentList(
            @PathVariable String stepNo) {
        return ResponseEntity.ok(ApiResponse.ok(equipmentService.getEquipmentList(stepNo)));
    }

    // 4.2 설비 파라미터·측정값
    @GetMapping("/api/equipments/{equipmentId}/parameters")
    public ResponseEntity<ApiResponse<EquipmentDto.ParameterListResponse>> getParameters(
            @PathVariable String equipmentId,
            @RequestParam(required = false) String period) {
        return ResponseEntity.ok(ApiResponse.ok(equipmentService.getParameters(equipmentId, period)));
    }

    // 4.3 설비 알람
    @GetMapping("/api/equipments/{equipmentId}/alarms")
    public ResponseEntity<ApiResponse<EquipmentDto.AlarmListResponse>> getAlarms(
            @PathVariable String equipmentId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.ok(equipmentService.getAlarms(equipmentId, status)));
    }

    // 4.4 설비 이벤트 로그
    @GetMapping("/api/equipments/{equipmentId}/events")
    public ResponseEntity<ApiResponse<EquipmentDto.EventListResponse>> getEvents(
            @PathVariable String equipmentId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        size = Math.min(size, 100);
        return ResponseEntity.ok(ApiResponse.ok(equipmentService.getEvents(equipmentId, page, size)));
    }

    // 4.5 관리자 의견 추가
    @PostMapping("/api/equipments/{equipmentId}/notes")
    public ResponseEntity<ApiResponse<EquipmentDto.NoteResponse>> addNote(
            @PathVariable String equipmentId,
            @RequestBody EquipmentDto.NoteRequest request) {
        // TODO: JWT 구현 후 실제 userId 사용
        String userId = "dev_user";
        return ResponseEntity.ok(ApiResponse.ok(
                equipmentService.addNote(equipmentId, request.getNoteText(), userId)));
    }
}