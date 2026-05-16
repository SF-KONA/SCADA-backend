package com.example.demo.domain.repository;

import com.example.demo.domain.entity.AiReportPdf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AiReportPdfRepository extends JpaRepository<AiReportPdf, Long> {

    Optional<AiReportPdf> findByJobId(String jobId);

    boolean existsByJobId(String jobId);

    @Modifying
    @Query("UPDATE AiReportPdf p SET p.downloadCount = p.downloadCount + 1 WHERE p.jobId = :jobId")
    void incrementDownloadCount(String jobId);
}
