package com.example.demo.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.example.demo.domain.dto.ReportQueryParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final WebClient webClient = WebClient.builder().build();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    // ── 1차 호출: 자연어 → 구조화 파라미터 ──────────────────────────────────
    public ReportQueryParams extractParams(String queryText) {
        String prompt = buildExtractionPrompt(queryText);
        String rawJson = callGemini(prompt);
        return parseParams(rawJson);
    }

    private String buildExtractionPrompt(String queryText) {
        return """
            당신은 반도체 공정 SCADA 시스템의 자연어 질의를 분석하는 파서입니다.
            사용자 질의를 분석해서 반드시 아래 JSON 형식만 반환하세요. 설명 없이 JSON만.

            지원 analysis_type:
            ALARM_FREQUENCY, EQUIPMENT_UPTIME, OEE_ANALYSIS, CRITICAL_ALARM,
            UNRESOLVED_ALARM, PARAMETER_TREND, PM_SCHEDULE, AI_SUGGESTION_HISTORY,
            ENV_ABNORMAL, PROCESS_OEE_COMPARE, EQUIPMENT_COMPARE, YIELD_IMPACT, UNACK_ALARM

            period.type: last_week | last_month | range
            filters.severity: INFO | WARN | ERR | null

            JSON 형식:
            {
              "analysis_type": "",
              "equipment_ids": [],
              "period": { "type": "last_week", "from": null, "to": null },
              "filters": { "severity": null, "status": null, "step_no": null },
              "sort": { "field": "oee", "order": "DESC" },
              "limit": null
            }

            사용자 질의: "%s"
            """.formatted(queryText);
    }

    // ── 2차 호출: 수치 데이터 → narrative 생성 ────────────────────────────────
    public String generateNarrative(String analysisType, String metricsJson) {
        String prompt = """
            당신은 반도체 공정 SCADA 시스템의 분석 결과를 해석하는 전문가입니다.
            아래 분석 수치를 바탕으로 운영자가 이해하기 쉬운 Markdown 요약 보고서를 작성하세요.
            - 분석 유형: %s
            - 주요 수치: %s

            요구사항:
            1. 핵심 인사이트 3줄 이내 요약 (## 요약)
            2. 주요 발견사항 bullet 형식 (## 주요 발견사항)
            3. 권고 조치 사항 (## 권고 사항)
            Markdown으로만 응답하세요.
            """.formatted(analysisType, metricsJson);

        return callGemini(prompt);
    }

    // ── 공통 Gemini 호출 ──────────────────────────────────────────────────────
    private String callGemini(String prompt) {
        Map<String, Object> body = Map.of(
            "contents", List.of(Map.of(
                "parts", List.of(Map.of("text", prompt))
            ))
        );

        try {
            Map<?, ?> response = webClient.post()
                .uri(apiUrl + "?key=" + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(30))
                .block();

            return extractText(response);
        } catch (Exception e) {
            log.error("Gemini API 호출 실패: {}", e.getMessage());
            throw new RuntimeException("Gemini 호출 실패: " + e.getMessage());
        }
    }

    private String extractText(Map<?, ?> response) {
        var candidates = (List<?>) response.get("candidates");
        var content    = (Map<?, ?>) ((Map<?, ?>) candidates.get(0)).get("content");
        var parts      = (List<?>) content.get("parts");
        return ((Map<?, ?>) parts.get(0)).get("text").toString().trim();
    }

    private ReportQueryParams parseParams(String rawJson) {
        // Gemini가 ```json ... ``` 감싸서 줄 수 있으므로 fence 제거
        String clean = rawJson
            .replaceAll("(?s)```json\\s*", "")
            .replaceAll("```", "")
            .trim();
        try {
            return objectMapper.readValue(clean, ReportQueryParams.class);
        } catch (Exception e) {
            log.error("파라미터 파싱 실패. raw: {}", rawJson);
            throw new IllegalStateException("Gemini 응답 파싱 실패");
        }
    }
}
