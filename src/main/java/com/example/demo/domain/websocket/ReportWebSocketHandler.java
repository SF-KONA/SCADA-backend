package com.example.demo.domain.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReportWebSocketHandler extends TextWebSocketHandler {

    private final ReportWebSocketService wsService;
    private final ObjectMapper           objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("[WS] 연결 수립: sessionId={}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode node = objectMapper.readTree(message.getPayload());
        String type  = node.path("type").asText();
        String jobId = node.path("job_id").asText();

        if ("SUBSCRIBE".equals(type) && !jobId.isBlank()) {
            wsService.subscribe(jobId, session);
            log.info("[WS] SUBSCRIBE 요청: jobId={}", jobId);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        wsService.unsubscribe(session);
        log.info("[WS] 연결 종료: sessionId={}", session.getId());
    }
}