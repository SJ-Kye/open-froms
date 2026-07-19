package com.openforms.common.openapi;

import com.openforms.common.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 요청 본문 검증 실패(400)를 Swagger 에 표기합니다. {@code @Valid} 위반은 {@code VALIDATION_FAILED} 이며
 * 필드별 사유가 {@code fieldErrors} 에 담깁니다.
 *
 * <p>도메인 값 규칙 위반(예: {@code OPTIONS_REQUIRED})처럼 조건이 엔드포인트마다 다른 400 은 이 어노테이션
 * 대신 해당 오퍼레이션에 {@code @ApiResponse} 로 직접 적어 조건을 드러냅니다.
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ApiResponse(responseCode = "400", description = "요청 본문 검증 실패 (VALIDATION_FAILED, fieldErrors 포함)",
        content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)))
public @interface ValidationErrors {
}
