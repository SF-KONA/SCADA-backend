package com.example.demo.domain.repository;

import com.example.demo.domain.entity.AiReportJob;
import com.example.demo.domain.enums.ReportJobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiReportJobRepository extends JpaRepository<AiReportJob, String> {

    // 목록 조회 - status 필터 없음
    Page<AiReportJob> findAllByOrderByRequestedAtDesc(Pageable pageable);

    // 목록 조회 - status 필터 있음
    Page<AiReportJob> findByStatusOrderByRequestedAtDesc(ReportJobStatus status, Pageable pageable);
}
