package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_report_pdfs")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiReportPdf {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;

    @Column(name = "job_id", nullable = false, length = 64)
    private String jobId;  // FK -> ai_report_jobs.job_id

    @Column(name = "pdf_path", nullable = false, length = 255)
    private String pdfPath;

    @Column(name = "generated_by", nullable = false, length = 16)
    private String generatedBy;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "download_count", nullable = false)
    @Builder.Default
    private Integer downloadCount = 0;
}
