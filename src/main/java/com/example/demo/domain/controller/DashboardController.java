package com.example.demo.domain.controller;

import com.example.demo.domain.common.ApiResponse;
import com.example.demo.domain.dto.DashboardDto;
import com.example.demo.domain.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    // 1.1 설비 현황 KPI
    @GetMapping("/kpi")
    public ResponseEntity<ApiResponse<DashboardDto.StatusResponse>> getStatus() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getStatus()));
    }

    // 1.2 OEE 요약
    @GetMapping("/oee")
    public ResponseEntity<ApiResponse<DashboardDto.OeeResponse>> getOee(
            @RequestParam(defaultValue = "today") String period) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getOee(period)));
    }

    // 1.3 공정별 이상률
    @GetMapping("/defect-rate")
    public ResponseEntity<ApiResponse<DashboardDto.DefectRateResponse>> getDefectRate(
            @RequestParam(defaultValue = "today") String period) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getDefectRate(period)));
    }

    // 1.4 테스트 양품률
    @GetMapping("/yield")
    public ResponseEntity<ApiResponse<DashboardDto.YieldResponse>> getYield(
            @RequestParam(defaultValue = "today") String period) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getYield(period)));
    }

    // 1.5 설비 목록 (stepNo, status 필터)
    @GetMapping("/equipments")
    public ResponseEntity<ApiResponse<DashboardDto.EquipmentsResponse>> getEquipments(
            @RequestParam(required = false) String stepNo,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getEquipments(stepNo, status)));
    }
}
