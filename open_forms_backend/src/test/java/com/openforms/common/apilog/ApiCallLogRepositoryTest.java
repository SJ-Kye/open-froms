package com.openforms.common.apilog;

import static org.assertj.core.api.Assertions.assertThat;

import com.openforms.common.apilog.domain.ApiCallLog;
import com.openforms.common.apilog.repository.ApiCallLogRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

/**
 * ApiCallLog 엔티티가 V2 스키마(api_call_logs)와 매핑 정합을 이루는지 검증합니다.
 * ddl-auto=validate + Flyway V2 가 이 테스트의 기동 자체로 확인되고, 저장/조회 왕복으로 컬럼 매핑을 확인합니다.
 */
@DataJpaTest
class ApiCallLogRepositoryTest {

    @Autowired
    private ApiCallLogRepository repository;

    @Test
    @DisplayName("이력 1건을 저장하면 IDENTITY PK 가 채워지고 필드가 그대로 조회된다")
    void savesAndReads() {
        ApiCallLog saved = repository.save(new ApiCallLog(
                "trace-abc", "GET", "/api/forms/10", 200, "creator@example.com", 12L,
                LocalDateTime.now()));

        assertThat(saved.getId()).isNotNull();

        ApiCallLog found = repository.findById(saved.getId()).orElseThrow();
        assertThat(found.getTraceId()).isEqualTo("trace-abc");
        assertThat(found.getMethod()).isEqualTo("GET");
        assertThat(found.getPath()).isEqualTo("/api/forms/10");
        assertThat(found.getStatus()).isEqualTo(200);
        assertThat(found.getPrincipal()).isEqualTo("creator@example.com");
        assertThat(found.getDurationMs()).isEqualTo(12L);
    }

    @Test
    @DisplayName("principal 은 nullable — 비인증 호출 이력도 저장된다")
    void allowsNullPrincipal() {
        ApiCallLog saved = repository.save(new ApiCallLog(
                "trace-xyz", "POST", "/api/public/forms/abc/responses", 201, null, 5L,
                LocalDateTime.now()));

        assertThat(repository.findById(saved.getId()).orElseThrow().getPrincipal()).isNull();
    }
}
