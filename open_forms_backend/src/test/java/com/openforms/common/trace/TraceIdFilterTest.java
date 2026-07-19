package com.openforms.common.trace;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * TraceIdFilter 가 요청마다 traceId 를 확보해 MDC·응답 헤더에 싣고, 요청 종료 시 MDC 를 비우는지 검증합니다.
 */
class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @Test
    @DisplayName("들어온 X-Request-Id 가 있으면 그 값을 이어받아 응답 헤더로 내보낸다")
    void reusesInboundId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceContext.REQUEST_ID_HEADER, "inbound-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(TraceContext.TRACE_ID_HEADER)).isEqualTo("inbound-123");
    }

    @Test
    @DisplayName("들어온 값이 없으면 새 traceId 를 발급하고, 요청 종료 후 MDC 를 비운다")
    void generatesWhenAbsent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                // 체인 실행 중에는 MDC 에 traceId 가 존재해야 로그에 실린다.
                assertThat(MDC.get(TraceContext.TRACE_ID_KEY)).isNotBlank();
            }
        };

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(TraceContext.TRACE_ID_HEADER)).isNotBlank();
        // 요청 종료 후에는 스레드 오염을 막기 위해 MDC 가 비워져야 한다.
        assertThat(MDC.get(TraceContext.TRACE_ID_KEY)).isNull();
    }
}
