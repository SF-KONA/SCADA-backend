package com.example.demo.domain.strategy;

import com.example.demo.domain.dto.ReportDto;
import com.example.demo.domain.dto.ReportQueryParams;

import java.util.List;
import java.util.Map;

// ── 분석 결과 내부 DTO ────────────────────────────────────────────────────────
public record AnalysisResult(
    List<ReportDto.ChartData>  charts,
    List<ReportDto.FmeaEntry>  fmea,
    Map<String, Object>        rawMetrics   // narrative 생성용 수치
) {}
