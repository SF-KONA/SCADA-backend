package com.example.demo.domain.controller;

import com.example.demo.domain.common.ApiResponse;
import com.example.demo.domain.dto.AlarmDto;
import com.example.demo.domain.dto.AlarmQueryParams;
import com.example.demo.domain.service.AlarmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/alarms")
@RequiredArgsConstructor
public class AlarmController {

    private final AlarmService alarmService;

    // 2.1 알람 목록 조회
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','LINE_MGR','WORKER')")
    public ResponseEntity<ApiResponse<AlarmDto.PageResponse>> getAlarms(
            @ModelAttribute AlarmQueryParams params
    ) {
        return ResponseEntity.ok(ApiResponse.ok(alarmService.getAlarms(params)));
    }

    // 2.6 미확인 비상 알람 조회 (경로 충돌 방지를 위해 /{alarmId} 보다 먼저 선언)
    @GetMapping("/emergency")
    @PreAuthorize("hasAnyRole('ADMIN','LINE_MGR','WORKER')")
    public ResponseEntity<ApiResponse<AlarmDto.EmergencyResponse>> getEmergencyAlarms() {
        return ResponseEntity.ok(ApiResponse.ok(alarmService.getEmergencyAlarms()));
    }

    // 2.2 알람 상세 조회
    @GetMapping("/{alarmId}")
    @PreAuthorize("hasAnyRole('ADMIN','LINE_MGR','WORKER')")
    public ResponseEntity<ApiResponse<AlarmDto.Detail>> getAlarmDetail(
            @PathVariable Long alarmId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(alarmService.getAlarmDetail(alarmId)));
    }

    // 2.3 알람 ACK
    @PatchMapping("/{alarmId}/ack")
    @PreAuthorize("hasAnyRole('ADMIN','LINE_MGR')")
    public ResponseEntity<ApiResponse<AlarmDto.AckResponse>> ackAlarm(
            @PathVariable Long alarmId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.ok(alarmService.ackAlarm(alarmId, userDetails.getUsername())));
    }

    // 2.4 처리 시작
    @PatchMapping("/{alarmId}/start")
    @PreAuthorize("hasAnyRole('ADMIN','LINE_MGR')")
    public ResponseEntity<ApiResponse<AlarmDto.StartResponse>> startAlarm(
            @PathVariable Long alarmId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.ok(alarmService.startAlarm(alarmId, userDetails.getUsername())));
    }

    // 2.5 처리 완료
    @PatchMapping("/{alarmId}/done")
    @PreAuthorize("hasAnyRole('ADMIN','LINE_MGR')")
    public ResponseEntity<ApiResponse<AlarmDto.DoneResponse>> doneAlarm(
            @PathVariable Long alarmId,
            @Valid @RequestBody AlarmDto.DoneRequest body,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                alarmService.doneAlarm(alarmId, userDetails.getUsername(), body.getComment())));
    }
}