package com.example.demo.domain.controller;

import com.example.demo.domain.common.ApiResponse;
import com.example.demo.domain.dto.EquipmentDto;
import com.example.demo.domain.service.EquipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class EquipmentController {

    private final EquipmentService equipmentService;

    @GetMapping("/api/processes/{stepNo}/equipments")
    public ResponseEntity<ApiResponse<EquipmentDto.EquipmentListResponse>> getEquipmentList(
            @PathVariable String stepNo) {
        return ResponseEntity.ok(ApiResponse.ok(equipmentService.getEquipmentList(stepNo)));
    }

    @GetMapping("/api/equipments/{equipmentId}/parameters")
    public ResponseEntity<ApiResponse<EquipmentDto.ParameterListResponse>> getParameters(
            @PathVariable String equipmentId,
            @RequestParam(required = false) String period) {
        return ResponseEntity.ok(ApiResponse.ok(equipmentService.getParameters(equipmentId, period)));
    }

    @GetMapping("/api/equipments/{equipmentId}/alarms")
    public ResponseEntity<ApiResponse<EquipmentDto.AlarmListResponse>> getAlarms(
            @PathVariable String equipmentId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.ok(equipmentService.getAlarms(equipmentId, status)));
    }

    @GetMapping("/api/equipments/{equipmentId}/events")
    public ResponseEntity<ApiResponse<EquipmentDto.EventListResponse>> getEvents(
            @PathVariable String equipmentId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        size = Math.min(size, 100);
        return ResponseEntity.ok(ApiResponse.ok(equipmentService.getEvents(equipmentId, page, size)));
    }

    @PostMapping("/api/equipments/{equipmentId}/notes")
    public ResponseEntity<ApiResponse<EquipmentDto.NoteResponse>> addNote(
            @PathVariable String equipmentId,
            @RequestBody EquipmentDto.NoteRequest request) {
        String userId = getAuthName();
        return ResponseEntity.ok(ApiResponse.ok(
                equipmentService.addNote(equipmentId, request.getNoteText(), userId)));
    }

    @GetMapping("/api/equipments/{equipmentId}/notes")
    public ResponseEntity<ApiResponse<EquipmentDto.NoteListResponse>> getNotes(
            @PathVariable String equipmentId) {
        return ResponseEntity.ok(ApiResponse.ok(equipmentService.getNotes(equipmentId)));
    }

    @PutMapping("/api/equipments/notes/{noteId}")
    public ResponseEntity<ApiResponse<EquipmentDto.NoteResponse>> updateNote(
            @PathVariable Long noteId,
            @RequestBody EquipmentDto.NoteUpdateRequest request) {
        String userId = getAuthName();
        return ResponseEntity.ok(ApiResponse.ok(
                equipmentService.updateNote(noteId, request.getNoteText(), userId)));
    }

    @DeleteMapping("/api/equipments/notes/{noteId}")
    public ResponseEntity<ApiResponse<Void>> deleteNote(@PathVariable Long noteId) {
        String userId = getAuthName();
        equipmentService.deleteNote(noteId, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    private String getAuthName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "unknown";
    }
}