package com.openforms.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI(springdoc) 문서 설정입니다. API 메타데이터와 JWT Bearer 보안 스킴을 정의해, Swagger UI 의
 * "Authorize" 로 토큰을 넣어 보호 엔드포인트를 시험할 수 있게 합니다.
 *
 * <p>전역 {@code SecurityRequirement} 는 두지 않습니다. 두면 회원가입/로그인·공개 폼 같은 익명 경로까지
 * 잠금으로 표기되므로, 보안 요구는 실제 인증이 필요한 오퍼레이션에만 어노테이션으로 붙입니다
 * ({@code @SecurityRequirement}). 이렇게 해야 API 설계 문서(05)의 🔒/🔓 표기와 일치합니다.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Open Forms API")
                        .description("설문/폼 서비스 API — 제작자는 폼을 만들어 공개 slug 로 발행하고, "
                                + "누구나 로그인 없이 익명으로 응답을 제출합니다.")
                        .version("v1"))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("로그인(/api/auth/login)으로 받은 액세스 토큰을 입력합니다.")));
    }
}
