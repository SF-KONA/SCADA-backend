package com.example.demo.domain.controller;

import com.example.demo.domain.common.ApiResponse;
import com.example.demo.domain.dto.ReportDto;
import com.example.demo.domain.service.ReportJobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.File;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportJobService reportJobService;

    // ── 1. POST /api/reports - 리포트 생성 요청 ───────────────────────────────
    // 명세서: 202 Accepted / Request: query_text / Response: job_id, status, requested_at
    @PostMapping
    public ResponseEntity<ApiResponse<ReportDto.CreateResponse>> createReport(
            @Valid @RequestBody ReportDto.CreateRequest request,
            @AuthenticationPrincipal UserDetails user) {

        ReportDto.CreateResponse response =
            reportJobService.createJob(user.getUsername(), request.getQueryText());

        // @Async - 바로 반환, 분석은 백그라운드
        reportJobService.processJob(response.getJobId());

        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(ApiResponse.ok(response));
    }

    // ── 3. GET /api/reports/{job_id} - 리포트 결과 조회 ──────────────────────
    // 명세서: narrative, charts, fmea, pdf 정보 포함
    @GetMapping("/{jobId}")
    public ResponseEntity<ApiResponse<ReportDto.ReportDetailResponse>> getReport(
            @PathVariable String jobId) {

        ReportDto.ReportDetailResponse response = reportJobService.getReport(jobId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // ── 4. GET /api/reports - 리포트 목록 조회 ───────────────────────────────
    // 명세서: page(기본1), size(기본20, 최대100), status 필터
    @GetMapping
    public ResponseEntity<ApiResponse<ReportDto.ReportListResponse>> getReportList(
            @RequestParam(defaultValue = "1")  int    page,
            @RequestParam(defaultValue = "20") int    size,
            @RequestParam(required = false)    String status) {

        size = Math.min(size, 100); // 최대 100
        ReportDto.ReportListResponse response =
            reportJobService.getReportList(page, size, status);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // ── 5. GET /api/reports/{job_id}/pdf - PDF 다운로드 ──────────────────────
    // 명세서: Content-Type: application/pdf / download_count +1
    @GetMapping("/{jobId}/pdf")
    public ResponseEntity<Resource> downloadPdf(
            @PathVariable String jobId,
            @AuthenticationPrincipal UserDetails user) {

        String pdfPath = reportJobService.getPdfPath(jobId, user.getUsername());

        File pdfFile = new File(pdfPath);
        Resource resource = new FileSystemResource(pdfFile);

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"report_" + jobId + ".pdf\"")
            .body(resource);
    }
}
