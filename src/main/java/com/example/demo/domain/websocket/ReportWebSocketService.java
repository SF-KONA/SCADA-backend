package com.example.demo.domain.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.demo.domain.entity.AiReportJob;
import com.example.demo.domain.enums.ReportJobStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 순수 WebSocket 기반 상태 push
 * 클라이언트가 SUBSCRIBE 메시지로 job_id를 등록하면
 * 상태 변경 시 해당 세션에 push
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ReportWebSocketService {

    private final ObjectMapper objectMapper;

    // jobId → WebSocketSession
    private final Map<String, WebSocketSession> subscriptions = new ConcurrentHashMap<>();

    /** 클라이언트가 SUBSCRIBE 메시지 보낼 때 등록 */
    public void subscribe(String jobId, WebSocketSession session) {
        subscriptions.put(jobId, session);
        log.info("[WS] 구독 등록: jobId={}, sessionId={}", jobId, session.getId());
    }

    /** 세션 종료 시 구독 정리 */
    public void unsubscribe(WebSocketSession session) {
        subscriptions.entrySet().removeIf(e -> e.getValue().getId().equals(session.getId()));
    }

    /**
     * 상태 변경 push
     * 명세서 기준 메시지 포맷:
     * { job_id, status, started_at, completed_at, error_message }
     */
    public void push(AiReportJob job) {
        WebSocketSession session = subscriptions.get(job.getJobId());
        if (session == null || !session.isOpen()) return;

        try {
            Map<String, Object> payload = new java.util.LinkedHashMap<>();
            payload.put("job_id",        job.getJobId());
            payload.put("status",        job.getStatus().name());
            payload.put("started_at",    job.getStartedAt() != null
                                            ? job.getStartedAt().toString() : null);
            payload.put("completed_at",  job.getCompletedAt() != null
                                            ? job.getCompletedAt().toString() : null);
            payload.put("error_message", job.getErrorMessage());

            String json = objectMapper.writeValueAsString(payload);
            session.sendMessage(new TextMessage(json));
            log.info("[WS] push: jobId={}, status={}", job.getJobId(), job.getStatus());

            // DONE / FAILED 이후엔 구독 해제
            if (job.getStatus() == ReportJobStatus.DONE
             || job.getStatus() == ReportJobStatus.FAILED) {
                subscriptions.remove(job.getJobId());
            }
        } catch (IOException e) {
            log.error("[WS] 전송 실패: jobId={}", job.getJobId(), e);
        }
    }
}
