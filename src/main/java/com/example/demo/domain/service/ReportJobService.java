package com.example.demo.domain.service;
import com.example.demo.domain.enums.ReportJobStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.demo.domain.common.ErrorCode;
import com.example.demo.domain.common.ReportException;
import com.example.demo.domain.dto.ReportDto;
import com.example.demo.domain.dto.ReportQueryParams;
import com.example.demo.domain.entity.AiReportJob;
import com.example.demo.domain.entity.AiReportPdf;
import com.example.demo.domain.repository.AiReportJobRepository;
import com.example.demo.domain.repository.AiReportPdfRepository;
import com.example.demo.domain.strategy.AnalysisDispatcher;
import com.example.demo.domain.strategy.AnalysisResult;
import com.example.demo.domain.websocket.ReportWebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportJobService {

    private final AiReportJobRepository  jobRepo;
    private final AiReportPdfRepository  pdfRepo;
    private final GeminiService          geminiService;
    private final AnalysisDispatcher     dispatcher;
    private final ReportWebSocketService wsService;
    private final ObjectMapper           objectMapper;

    // ── 1. 잡 생성 (POST /api/reports) ───────────────────────────────────────
    @Transactional
    public ReportDto.CreateResponse createJob(String userId, String queryText) {
        String jobId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        AiReportJob job = AiReportJob.builder()
            .jobId(jobId)
            .userId(userId)
            .queryText(queryText)
            .status(ReportJobStatus.QUEUED)
            .requestedAt(now)
            .build();
        jobRepo.save(job);

        return ReportDto.CreateResponse.builder()
            .jobId(jobId)
            .status("QUEUED")
            .requestedAt(now)
            .build();
    }

    // ── 2. 비동기 분석 실행 ───────────────────────────────────────────────────
    @Async("reportTaskExecutor")
    @Transactional
    public void processJob(String jobId) {
        AiReportJob job = jobRepo.findById(jobId).orElseThrow();

        try {
            // RUNNING 전환
            job.setStatus(ReportJobStatus.RUNNING);
            job.setStartedAt(LocalDateTime.now());
            jobRepo.save(job);
            wsService.push(job);

            // 1차 Gemini: 자연어 → 파라미터 추출
            ReportQueryParams params = geminiService.extractParams(job.getQueryText());
            log.info("[{}] 파라미터 추출: {}", jobId, params.getAnalysisType());

            // 규칙 기반 분석
            AnalysisResult result = dispatcher.dispatch(params);

            // 2차 Gemini: 수치 → narrative 생성
            String metricsJson = objectMapper.writeValueAsString(result.rawMetrics());
            String narrative   = geminiService.generateNarrative(params.getAnalysisType(), metricsJson);

            // 결과 저장
            job.setNarrative(narrative);
            job.setCharts(objectMapper.writeValueAsString(result.charts()));
            job.setFmea(objectMapper.writeValueAsString(result.fmea()));
            job.setStatus(ReportJobStatus.DONE);
            job.setCompletedAt(LocalDateTime.now());
            jobRepo.save(job);

            wsService.push(job);
            log.info("[{}] 분석 완료", jobId);

        } catch (Exception e) {
            log.error("[{}] 분석 실패: {}", jobId, e.getMessage());
            job.setStatus(ReportJobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setCompletedAt(LocalDateTime.now());
            jobRepo.save(job);
            wsService.push(job);
        }
    }

    // ── 3. 결과 조회 (GET /api/reports/{job_id}) ──────────────────────────────
    @Transactional(readOnly = true)
    public ReportDto.ReportDetailResponse getReport(String jobId) {
        AiReportJob job = jobRepo.findById(jobId)
            .orElseThrow(() -> new ReportException(ErrorCode.REPORT_NOT_FOUND));

        // PDF 정보
        ReportDto.PdfInfo pdfInfo = pdfRepo.findByJobId(jobId)
            .map(p -> ReportDto.PdfInfo.builder()
                .reportId(p.getReportId())
                .downloadCount(p.getDownloadCount())
                .generatedAt(p.getGeneratedAt())
                .build())
            .orElse(null);

        // charts, fmea JSON 파싱 (DONE일 때만)
        List<ReportDto.ChartData> charts = null;
        List<ReportDto.FmeaEntry> fmea   = null;
        if (job.getStatus() == ReportJobStatus.DONE) {
            try {
                if (job.getCharts() != null) {
                    charts = objectMapper.readValue(job.getCharts(),
                        objectMapper.getTypeFactory().constructCollectionType(
                            List.class, ReportDto.ChartData.class));
                }
                if (job.getFmea() != null) {
                    fmea = objectMapper.readValue(job.getFmea(),
                        objectMapper.getTypeFactory().constructCollectionType(
                            List.class, ReportDto.FmeaEntry.class));
                }
            } catch (Exception e) {
                log.error("[{}] 결과 파싱 실패", jobId, e);
            }
        }

        return ReportDto.ReportDetailResponse.builder()
            .jobId(job.getJobId())
            .queryText(job.getQueryText())
            .status(job.getStatus().name())
            .narrative(job.getNarrative())
            .charts(charts)
            .fmea(fmea)
            .requestedAt(job.getRequestedAt())
            .startedAt(job.getStartedAt())
            .completedAt(job.getCompletedAt())
            .errorMessage(job.getErrorMessage())
            .pdf(pdfInfo)
            .build();
    }

    // ── 4. 목록 조회 (GET /api/reports) ──────────────────────────────────────
    @Transactional(readOnly = true)
    public ReportDto.ReportListResponse getReportList(int page, int size, String status) {
        // page는 1-based (명세서), PageRequest는 0-based
        PageRequest pageable = PageRequest.of(page - 1, size);

        Page<AiReportJob> pageResult;
        if (status != null && !status.isBlank()) {
            ReportJobStatus jobStatus = ReportJobStatus.valueOf(status);
            pageResult = jobRepo.findByStatusOrderByRequestedAtDesc(jobStatus, pageable);
        } else {
            pageResult = jobRepo.findAllByOrderByRequestedAtDesc(pageable);
        }

        List<ReportDto.ReportListItem> items = pageResult.getContent().stream()
            .map(job -> ReportDto.ReportListItem.builder()
                .jobId(job.getJobId())
                .queryText(job.getQueryText())
                .status(job.getStatus().name())
                .requestedAt(job.getRequestedAt())
                .completedAt(job.getCompletedAt())
                .hasPdf(pdfRepo.existsByJobId(job.getJobId()))
                .build())
            .toList();

        return ReportDto.ReportListResponse.builder()
            .total(pageResult.getTotalElements())
            .page(page)
            .size(size)
            .totalPages(pageResult.getTotalPages())
            .items(items)
            .build();
    }

    // ── 5. PDF 다운로드용 경로 조회 (GET /api/reports/{job_id}/pdf) ────────────
    @Transactional
    public String getPdfPath(String jobId, String userId) {
        AiReportJob job = jobRepo.findById(jobId)
            .orElseThrow(() -> new ReportException(ErrorCode.REPORT_NOT_FOUND));

        if (job.getStatus() != ReportJobStatus.DONE) {
            throw new ReportException(ErrorCode.REPORT_NOT_DONE);
        }

        AiReportPdf pdf = pdfRepo.findByJobId(jobId)
            .orElseGet(() -> generateAndSavePdf(job, userId)); // 최초 요청 시 생성

        pdfRepo.incrementDownloadCount(jobId);
        return pdf.getPdfPath();
    }

    /** PDF 최초 생성 후 DB 저장 */
    private AiReportPdf generateAndSavePdf(AiReportJob job, String userId) {
        // TODO: PDF 생성 라이브러리(iText / Flying Saucer 등) 연동
        String pdfPath = "/reports/pdf/" + job.getJobId() + ".pdf";

        AiReportPdf pdf = AiReportPdf.builder()
            .jobId(job.getJobId())
            .pdfPath(pdfPath)
            .generatedBy(userId)
            .generatedAt(LocalDateTime.now())
            .downloadCount(0)
            .build();
        return pdfRepo.save(pdf);
    }
}
