package com.example.demo.domain.entity;

import com.example.demo.domain.enums.AlarmSeverity;
import com.example.demo.domain.enums.AlarmSourceType;
import com.example.demo.domain.enums.AlarmStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "alarms",
        indexes = {
                @Index(name = "idx_alarm_status", columnList = "status"),
                @Index(name = "idx_alarm_occurred", columnList = "occurred_at")
        })
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alarm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alarm_id")
    private Long alarmId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private AlarmSourceType sourceType;

    @Column(name = "equipment_param_id")
    private Long equipmentParamId;  // source_type=EQP일 때

    @Column(name = "environment_param_id")
    private Long environmentParamId;  // source_type=ENV일 때

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlarmSeverity severity;

    @Column(length = 255)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AlarmStatus status = AlarmStatus.NEW;

    @Column(name = "ack_user_id", length = 16)
    private String ackUserId;

    @Column(name = "ack_at")
    private LocalDateTime ackAt;

    @Column(name = "start_user_id", length = 16)
    private String startUserId;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "done_user_id", length = 16)
    private String doneUserId;

    @Column(name = "done_at")
    private LocalDateTime doneAt;

    @Column(name = "last_occurred_at")
    private LocalDateTime lastOccurredAt;  // dedup: 동일 source 재발생 시 갱신

    @Column(name = "occurrence_count", nullable = false)
    private Integer occurrenceCount;  // 알람 반복 발생 횟수

    @Column(name = "triggered_value")
    private Double triggeredValue;  // 알람 발생 시 측정값

    @Column(name = "done_comment", length = 500)
    private String doneComment;  // 처리 완료 코멘트
}