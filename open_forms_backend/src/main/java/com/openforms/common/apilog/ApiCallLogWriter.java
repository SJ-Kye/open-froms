package com.openforms.common.apilog;

import com.openforms.common.apilog.domain.ApiCallLog;
import com.openforms.common.apilog.repository.ApiCallLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * API 호출 이력을 비동기로 저장합니다. 본 요청 스레드와 분리되어 응답 지연에 영향을 주지 않으며,
 * 저장이 실패해도 요청 처리에는 영향이 없도록 예외를 삼키고 로그만 남깁니다.
 */
@Component
public class ApiCallLogWriter {

    private static final Logger log = LoggerFactory.getLogger(ApiCallLogWriter.class);

    private final ApiCallLogRepository repository;

    public ApiCallLogWriter(ApiCallLogRepository repository) {
        this.repository = repository;
    }

    @Async("apiCallLogExecutor")
    public void write(ApiCallLog entry) {
        try {
            repository.save(entry);
        } catch (Exception e) {
            log.warn("API 호출 이력 저장 실패: {} {}", entry.getMethod(), entry.getPath(), e);
        }
    }
}
