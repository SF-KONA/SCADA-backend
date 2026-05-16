package com.example.demo.domain.strategy;

import com.example.demo.domain.dto.ReportQueryParams;

public interface AnalysisStrategy {
    String getType();
    AnalysisResult analyze(ReportQueryParams params);
}
