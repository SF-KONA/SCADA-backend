package com.example.demo.domain.strategy;

import com.example.demo.domain.dto.ReportQueryParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalysisDispatcher {

    private final Map<String, AnalysisStrategy> strategies;

    @Autowired
    public AnalysisDispatcher(List<AnalysisStrategy> strategyList) {
        this.strategies = strategyList.stream()
            .collect(Collectors.toMap(AnalysisStrategy::getType, s -> s));
    }

    public AnalysisResult dispatch(ReportQueryParams params) {
        AnalysisStrategy strategy = strategies.get(params.getAnalysisType());
        if (strategy == null) {
            throw new IllegalArgumentException("지원하지 않는 분석 유형: " + params.getAnalysisType());
        }
        return strategy.analyze(params);
    }
}
