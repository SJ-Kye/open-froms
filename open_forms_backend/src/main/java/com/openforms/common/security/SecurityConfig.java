package com.openforms.common.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 무상태 JWT 기반 보안 설정입니다.
 *
 * <ul>
 *   <li>세션 미사용(STATELESS), CSRF·폼로그인·HTTP Basic 비활성.</li>
 *   <li>익명 허용: 회원가입/로그인({@code /api/auth/register}·{@code /login}), 공개 폼
 *       ({@code /api/public/**}), Swagger, 에러. {@code /api/auth/me} 는 인증이 필요합니다.</li>
 *   <li>그 외는 인증 필요 — {@link JwtAuthenticationFilter} 가 Bearer 토큰을 검증해 인증을 주입.</li>
 *   <li>미인증 401·인가 실패 403 은 공통 에러 포맷으로 통일({@link RestAuthenticationEntryPoint}·
 *       {@link RestAccessDeniedHandler}).</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    private final JwtTokenProvider tokenProvider;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(JwtTokenProvider tokenProvider,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler) {
        this.tokenProvider = tokenProvider;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 회원가입·로그인만 익명 허용. /api/auth/me 는 인증이 필요하므로 /api/auth/** 로
                        // 통째로 열지 않습니다(그러면 /me 가 미인증으로 통과해 principal 이 없습니다).
                        .requestMatchers("/api/auth/register", "/api/auth/login", "/api/public/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/error", "/favicon.ico").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(new JwtAuthenticationFilter(tokenProvider),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** 회원가입 시 비밀번호 해싱·로그인 시 대조에 사용합니다(BCrypt). */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
