package com.example.demo.domain.scheduler;

import com.example.demo.domain.service.SuggestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 30초마다 모든 설비의 controllable 파라미터를 스캔하여 룰베이스 제안을 생성.
 * 수동 트리거(/api/admin/suggestions/generate)와 동일한 SuggestionService 메서드를 호출.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SuggestionGeneratorScheduler {

    private final SuggestionService suggestionService;

    @Scheduled(fixedDelayString = "${suggestion.generator.interval-ms:300000}", initialDelay = 30000)
    public void generate() {
        try {
            int created = suggestionService.generateForAll();
            if (created > 0) {
                log.info("[SuggestionGenerator] created {} suggestions", created);
            }
        } catch (Exception e) {
            log.error("[SuggestionGenerator] failed", e);
        }
    }
}
