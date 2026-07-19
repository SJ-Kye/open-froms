package com.openforms.common.apilog;

import com.openforms.common.apilog.domain.ApiCallLog;
import com.openforms.common.trace.TraceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 처리된 모든 요청의 메타데이터를 모아 비동기로 이력에 남깁니다.
 *
 * <p>{@code afterCompletion} 은 예외 처리(GlobalExceptionHandler 가 상태 코드를 세팅한 뒤)까지 끝난
 * 시점에 호출되므로 4xx·5xx 응답도 그대로 기록됩니다.
 */
@Component
public class ApiCallLogInterceptor implements HandlerInterceptor {

    private static final String START_NANOS = ApiCallLogInterceptor.class.getName() + ".startNanos";

    private final ApiCallLogWriter writer;

    public ApiCallLogInterceptor(ApiCallLogWriter writer) {
        this.writer = writer;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_NANOS, System.nanoTime());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
            Exception ex) {
        writer.write(toEntry(request, response));
    }

    private ApiCallLog toEntry(HttpServletRequest request, HttpServletResponse response) {
        long durationMs = elapsedMillis(request);
        return new ApiCallLog(
                TraceContext.currentTraceId(),
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                currentPrincipal(),
                durationMs,
                LocalDateTime.now());
    }

    private long elapsedMillis(HttpServletRequest request) {
        Object start = request.getAttribute(START_NANOS);
        if (start instanceof Long startNanos) {
            return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
        }
        return 0L;
    }

    /** 인증된 요청이면 주체 이름(이메일), 아니면 null 입니다(비인증 호출은 principal 없음). */
    private String currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authentication.getName();
    }
}
