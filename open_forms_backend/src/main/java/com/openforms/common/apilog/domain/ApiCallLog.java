package com.openforms.common.apilog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * API 호출 이력 1건입니다(메타데이터만 — 본문·비밀번호·JWT 미포함).
 *
 * <p>도메인 엔티티가 아니라 운영 로그이므로 감사 베이스를 상속하지 않고 자체 {@code createdAt}(호출
 * 시각)만 둡니다. {@code principal} 은 인증 시 이메일, 비인증이면 null 입니다.
 */
@Entity
@Table(name = "api_call_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiCallLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trace_id", nullable = false, length = 64)
    private String traceId;

    @Column(name = "method", nullable = false, length = 10)
    private String method;

    @Column(name = "path", nullable = false, length = 500)
    private String path;

    @Column(name = "status", nullable = false)
    private int status;

    @Column(name = "principal", length = 100)
    private String principal;

    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public ApiCallLog(String traceId, String method, String path, int status, String principal,
            long durationMs, LocalDateTime createdAt) {
        this.traceId = traceId;
        this.method = method;
        this.path = path;
        this.status = status;
        this.principal = principal;
        this.durationMs = durationMs;
        this.createdAt = createdAt;
    }
}
