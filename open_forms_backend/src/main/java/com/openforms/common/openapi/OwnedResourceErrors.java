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
 * 제작자 소유 리소스에 접근하는 오퍼레이션의 공통 실패 응답(401·403·404)을 Swagger 에 표기합니다.
 *
 * <p>401(미인증)과 403(소유자 아님)을 구분해 답하는 것이 이 서비스의 규약이고, 404 는 존재하지 않는
 * 리소스입니다. 소유 검사 순서(404 → 403)는 {@code FormAccessGuard} 에 있습니다.
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ApiResponse(responseCode = "401", description = "토큰이 없거나 유효하지 않음 (UNAUTHORIZED)",
        content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)))
@ApiResponse(responseCode = "403", description = "소유자가 아님 (ACCESS_DENIED)",
        content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)))
@ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음",
        content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)))
public @interface OwnedResourceErrors {
}
