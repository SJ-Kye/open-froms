package com.openforms.form.controller;

import com.openforms.common.exception.ErrorResponse;
import com.openforms.form.dto.PublicFormResponse;
import com.openforms.form.service.PublicFormService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 공개 폼 조회 엔드포인트입니다. <b>인증이 필요 없으므로</b> {@code @SecurityRequirement} 를 붙이지 않습니다
 * (Swagger 에서도 자물쇠 없이 표시됩니다).
 */
@Tag(name = "공개 폼", description = "공개 링크(slug)로 폼을 조회합니다 — 인증 불필요")
@RestController
@RequestMapping("/api/public/forms")
public class PublicFormController {

    private final PublicFormService publicFormService;

    public PublicFormController(PublicFormService publicFormService) {
        this.publicFormService = publicFormService;
    }

    @Operation(summary = "공개 폼 조회",
            description = "발행되었거나 종료된 폼을 반환합니다. 아직 발행되지 않았거나 없는 slug 는 404 입니다.")
    @ApiResponse(responseCode = "200", description = "공개 폼 (status 로 PUBLISHED·CLOSED 구분)")
    @ApiResponse(responseCode = "404", description = "없는 slug 이거나 아직 발행되지 않음 (FORM_NOT_FOUND)",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{slug}")
    public PublicFormResponse get(@PathVariable String slug) {
        return publicFormService.get(slug);
    }
}
