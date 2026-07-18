package com.openforms.common;

import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * JPA Auditing 활성화 및 감사 주체(created_by/updated_by) 해석기입니다.
 *
 * <p>인증된 요청이면 인증 주체 이름(이메일)을, 그렇지 않으면 {@code ANONYMOUS} 를 반환합니다.
 * 익명 응답 제출과 비로그인 회원가입이 모두 {@code ANONYMOUS} 로 기록되는 이유입니다.
 * 보안 필터가 아직 연결되지 않은 현재는 항상 {@code ANONYMOUS} 를 반환합니다.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {

    static final String ANONYMOUS = "ANONYMOUS";

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null
                    || !authentication.isAuthenticated()
                    || authentication instanceof AnonymousAuthenticationToken) {
                return Optional.of(ANONYMOUS);
            }
            return Optional.of(authentication.getName());
        };
    }
}
