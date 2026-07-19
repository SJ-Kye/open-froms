package com.openforms.common.exception;

import com.openforms.common.trace.TraceContext;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;

/**
 * 모든 에러 응답의 공통 포맷입니다(성공 응답은 각 API 의 bare 본문을 그대로 사용합니다).
 *
 * <p>{@code traceId} 는 현재 요청의 추적 식별자로, 응답 헤더 {@code X-Trace-Id} 와 동일한 값이며 서버
 * 로그와 대조할 수 있게 합니다. 포맷 정의는 {@code dev-docs/05-api-design.md} 「공통 에러 포맷」 참고.
 *
 * @param fieldErrors 검증 실패(400) 시 필드별 사유. 그 외에는 빈 리스트입니다.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        String traceId,
        List<FieldError> fieldErrors) {

    /** 검증 실패 시 필드별 사유 한 건입니다. */
    public record FieldError(String field, String message) {
    }

    public static ErrorResponse of(HttpStatus status, String code, String message, String path) {
        return of(status, code, message, path, List.of());
    }

    public static ErrorResponse of(HttpStatus status, String code, String message, String path,
            List<FieldError> fieldErrors) {
        return new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                path,
                TraceContext.currentTraceId(),
                fieldErrors);
    }
}
