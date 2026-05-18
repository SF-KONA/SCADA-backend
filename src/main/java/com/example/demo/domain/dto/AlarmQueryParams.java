package com.example.demo.domain.dto;

import com.example.demo.domain.enums.AlarmSeverity;
import com.example.demo.domain.enums.AlarmSourceType;
import com.example.demo.domain.enums.AlarmStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class AlarmQueryParams {

    private int page = 1;
    private int size = 20;

    /** 공정 필터 (02~07) */
    private String stepNo;

    /** 구역 필터 (FRONT/BACK/EQP) — ENV 알람용 */
    private String zoneCode;

    /**
     * 알람 상태 (복수 콤마 구분, 예: NEW,ACK)
     * 컨트롤러에서 파싱 후 List<AlarmStatus> 로 변환
     */
    private String status;

    /**
     * 심각도 (복수 콤마 구분, 예: WARN,ERR)
     * 컨트롤러에서 파싱 후 List<AlarmSeverity> 로 변환
     */
    private String severity;

    /** EQP / ENV */
    private AlarmSourceType sourceType;

    /** 설비 ID 필터 */
    private String equipmentId;

    /** 발생 시각 범위 시작 */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime from;

    /** 발생 시각 범위 종료 */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime to;

    // ─── 파싱 헬퍼 ──────────────────────────────

    public List<AlarmStatus> parsedStatuses() {
        if (status == null || status.isBlank()) return null;
        return List.of(status.split(","))
                .stream()
                .map(String::trim)
                .map(AlarmStatus::valueOf)
                .toList();
    }

    public List<AlarmSeverity> parsedSeverities() {
        if (severity == null || severity.isBlank()) return null;
        return List.of(severity.split(","))
                .stream()
                .map(String::trim)
                .map(AlarmSeverity::valueOf)
                .toList();
    }

    /** page/size 유효성 검사 (400 throw는 서비스에서) */
    public void validate() {
        if (page < 1) throw new IllegalArgumentException("page는 1 이상이어야 합니다.");
        if (size < 1 || size > 100) throw new IllegalArgumentException("size는 1~100 사이여야 합니다.");
    }
}