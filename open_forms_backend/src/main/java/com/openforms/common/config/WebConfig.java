package com.openforms.common.config;

import com.openforms.common.apilog.ApiCallLogInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 설정입니다. API 호출 이력 인터셉터를 등록하되, 문서/정적/에러 경로는 제외해 노이즈와
 * 불필요한 이력 적재를 막습니다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ApiCallLogInterceptor apiCallLogInterceptor;

    public WebConfig(ApiCallLogInterceptor apiCallLogInterceptor) {
        this.apiCallLogInterceptor = apiCallLogInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiCallLogInterceptor)
                .excludePathPatterns(
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/error",
                        "/favicon.ico");
    }
}
