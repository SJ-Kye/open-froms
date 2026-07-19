package com.openforms.common.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 비동기 실행 설정입니다. API 호출 이력 저장({@code ApiCallLogWriter})처럼 요청 처리와 분리해야 하는
 * 작업이 사용할 소형 전용 풀을 제공합니다. 큐가 차면 호출 스레드가 직접 처리하도록 두어(로그 유실 방지)
 * 하되, 이력 저장 실패가 본 요청에 영향을 주지는 않습니다.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "apiCallLogExecutor")
    public Executor apiCallLogExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("api-call-log-");
        executor.initialize();
        return executor;
    }
}
