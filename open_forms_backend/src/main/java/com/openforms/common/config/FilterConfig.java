package com.openforms.common.config;

import com.openforms.common.trace.TraceIdFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * 서블릿 필터 등록 설정입니다.
 *
 * <p>{@link TraceIdFilter} 를 {@link Ordered#HIGHEST_PRECEDENCE} 로 올려 스프링 시큐리티 필터 체인보다
 * 먼저 실행되게 합니다. 그래야 인증/인가 처리 단계의 로그에도 traceId 가 포함됩니다.
 */
@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilter() {
        FilterRegistrationBean<TraceIdFilter> registration = new FilterRegistrationBean<>(new TraceIdFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/*");
        return registration;
    }
}
