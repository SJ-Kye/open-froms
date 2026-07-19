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
 * 인증이 필요한 오퍼레이션의 공통 실패 응답(401)을 Swagger 에 표기합니다.
 *
 * <p>같은 {@code @ApiResponse} 블록을 오퍼레이션마다 되풀이하면 문서와 코드가 각각 어긋나기 쉬우므로,
 * 반복되는 조합을 합성 어노테이션 한 곳에 모읍니다. 실제 표기 여부는
 * {@code ApiContractConsistencyTest} 가 OpenAPI 스펙으로 검증합니다.
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ApiResponse(responseCode = "401", description = "토큰이 없거나 유효하지 않음 (UNAUTHORIZED)",
        content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)))
public @interface AuthenticatedErrors {
}
