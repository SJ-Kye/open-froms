package com.openforms.common.trace;

import org.slf4j.MDC;

/**
 * 요청별 추적 식별자(traceId)의 보관소입니다. 값은 {@link TraceIdFilter} 가 요청 시작 시 MDC 에
 * 넣고 종료 시 제거하며, 로그 패턴({@code %X{traceId}})·에러 응답·호출 이력이 이곳을 통해 공유합니다.
 *
 * <p>MDC 를 단일 저장소로 삼아 스레드로컬을 이중으로 두지 않습니다.
 */
public final class TraceContext {

    /** 요청/응답 헤더 이름입니다. 클라이언트가 보낸 값이 있으면 그대로 이어받습니다. */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    /** 클라이언트가 관례적으로 보내는 대체 헤더 이름입니다. */
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    /** MDC 키이자 로그 패턴에서 참조하는 이름입니다. */
    public static final String TRACE_ID_KEY = "traceId";

    private TraceContext() {
    }

    /** 현재 요청의 traceId 입니다. 필터 밖(요청 컨텍스트 없음)에서는 null 일 수 있습니다. */
    public static String currentTraceId() {
        return MDC.get(TRACE_ID_KEY);
    }
}
