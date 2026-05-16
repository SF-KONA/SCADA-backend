package com.example.demo.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Gemini 1차 호출 결과: 자연어 → 구조화 파라미터
 */
@Data
public class ReportQueryParams {

    @JsonProperty("analysis_type")
    private String analysisType;

    @JsonProperty("equipment_ids")
    private List<String> equipmentIds;

    private Period period;
    private Filters filters;
    private Sort sort;
    private Integer limit;

    @Data
    public static class Period {
        private String type;           // last_week | last_month | range
        private LocalDateTime from;
        private LocalDateTime to;
    }

    @Data
    public static class Filters {
        private String severity;       // INFO | WARN | ERR | null
        private String status;
        @JsonProperty("step_no")
        private String stepNo;
    }

    @Data
    public static class Sort {
        private String field;
        private String order;          // ASC | DESC
    }
}
