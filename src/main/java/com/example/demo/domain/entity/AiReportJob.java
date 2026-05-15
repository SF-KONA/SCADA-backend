package com.example.demo.domain.entity;
import com.example.demo.domain.enums.ReportJobStatus;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_report_jobs")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiReportJob {

    @Id
    @Column(name = "job_id", length = 64)
    private String jobId;  // UUID

    @Column(name = "user_id", nullable = false, length = 16)
    private String userId;

    @Column(name = "query_text", nullable = false, length = 500)
    private String queryText;  // 자연어 질의

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReportJobStatus status = ReportJobStatus.QUEUED;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String narrative;  // markdown 분석 요약

    /** JSON {type, title, data} — String으로 보관, Jackson으로 변환 */
    @Column(columnDefinition = "JSON")
    private String charts;

    /** JSON {failure_mode, effect, cause, S, O, D, rpn, action} */
    @Column(columnDefinition = "JSON")
    private String fmea;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_message", length = 500)
    private String errorMessage;
}
