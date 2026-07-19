package com.openforms.common.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 요청마다 traceId 를 확보해 MDC 와 응답 헤더에 실어 주는 필터입니다.
 *
 * <p>클라이언트가 {@code X-Trace-Id}(또는 {@code X-Request-Id}) 를 보내면 그 값을 이어받아 분산 추적을
 * 연결하고, 없으면 UUID 를 새로 발급합니다. MDC 에 넣으므로 이 요청에서 남는 모든 로그에 traceId 가
 * 자동으로 붙고, 응답 헤더로 내보내 호출자가 자신의 요청을 서버 로그와 대조할 수 있습니다.
 *
 * <p>보안 필터 체인보다 먼저 실행되도록 최우선 순서로 등록합니다(FilterConfig 참고). 그래야 인증
 * 실패 로그에도 traceId 가 남습니다.
 */
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String traceId = resolveTraceId(request);
        MDC.put(TraceContext.TRACE_ID_KEY, traceId);
        response.setHeader(TraceContext.TRACE_ID_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TraceContext.TRACE_ID_KEY);
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        String inbound = request.getHeader(TraceContext.TRACE_ID_HEADER);
        if (!StringUtils.hasText(inbound)) {
            inbound = request.getHeader(TraceContext.REQUEST_ID_HEADER);
        }
        return StringUtils.hasText(inbound) ? inbound : UUID.randomUUID().toString();
    }
}
