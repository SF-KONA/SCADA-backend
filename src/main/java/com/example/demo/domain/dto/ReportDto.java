package com.example.demo.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

public class ReportDto {

    // ── 1. POST /api/reports ─────────────────────────────────────────────────

    /** Request */
    @Getter
    public static class CreateRequest {
        @NotBlank(message = "query_text는 필수입니다")
        @Size(max = 500, message = "query_text는 최대 500자입니다")
        @JsonProperty("query_text")
        private String queryText;
    }

    /** Response (202) */
    @Data @Builder
    public static class CreateResponse {
        @JsonProperty("job_id")
        private String jobId;

        private String status;

        @JsonProperty("requested_at")
        private LocalDateTime requestedAt;
    }

    // ── 3. GET /api/reports/{job_id} ─────────────────────────────────────────

    @Data @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ReportDetailResponse {
        @JsonProperty("job_id")
        private String jobId;

        @JsonProperty("query_text")
        private String queryText;

        private String status;
        private String narrative;
        private List<ChartData> charts;
        private List<FmeaEntry> fmea;

        @JsonProperty("requested_at")
        private LocalDateTime requestedAt;

        @JsonProperty("started_at")
        private LocalDateTime startedAt;

        @JsonProperty("completed_at")
        private LocalDateTime completedAt;

        @JsonProperty("error_message")
        private String errorMessage;

        private PdfInfo pdf;
    }

    /** 차트 데이터 */
    @Data @Builder
    public static class ChartData {
        private String type;   // line | bar | pie
        private String title;
        private Object data;   // { labels:[], datasets:[] } or { labels:[], values:[] }
    }

    /** FMEA 항목 - 명세서 기준 snake_case + 대문자 S/O/D */
    @Data @Builder
    public static class FmeaEntry {
        @JsonProperty("failure_mode")
        private String failureMode;

        private String effect;
        private String cause;

        @JsonProperty("S")
        private int s;

        @JsonProperty("O")
        private int o;

        @JsonProperty("D")
        private int d;

        private int rpn;
        private String action;
    }

    /** PDF 정보 */
    @Data @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PdfInfo {
        @JsonProperty("report_id")
        private Long reportId;

        @JsonProperty("download_count")
        private Integer downloadCount;

        @JsonProperty("generated_at")
        private LocalDateTime generatedAt;
    }

    // ── 4. GET /api/reports (목록) ───────────────────────────────────────────

    @Data @Builder
    public static class ReportListResponse {
        private long total;
        private int page;
        private int size;

        @JsonProperty("total_pages")
        private int totalPages;

        private List<ReportListItem> items;
    }

    @Data @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ReportListItem {
        @JsonProperty("job_id")
        private String jobId;

        @JsonProperty("query_text")
        private String queryText;

        private String status;

        @JsonProperty("requested_at")
        private LocalDateTime requestedAt;

        @JsonProperty("completed_at")
        private LocalDateTime completedAt;

        @JsonProperty("has_pdf")
        private boolean hasPdf;
    }
}
