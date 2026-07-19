package com.openforms.form.controller;

import com.openforms.common.exception.ErrorResponse;
import com.openforms.common.openapi.OwnedResourceErrors;
import com.openforms.form.dto.QuestionRequest;
import com.openforms.form.dto.QuestionResponse;
import com.openforms.form.service.QuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 폼 하위 리소스인 질문의 CRUD 엔드포인트입니다. 모두 인증·소유자 전용이며, 폼이 DRAFT 일 때만 편집됩니다.
 * HTTP 매핑·검증·상태 코드만 담당하고 규칙은 {@link QuestionService} 에 위임합니다.
 */
@Tag(name = "질문", description = "폼 하위 질문 추가·수정·삭제(DRAFT 폼, 제작자 소유)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/forms/{formId}/questions")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @Operation(summary = "질문 추가", description = "폼에 질문을 추가합니다. 선택형은 선택지 2개 이상, RATING/NUMBER 는 min≤max.")
    // ResponseEntity 로 내는 201 은 springdoc 이 추론하지 못하므로 직접 적습니다(폼 생성과 동일).
    @ApiResponse(responseCode = "201", description = "생성됨 (Location 헤더에 질문 경로)",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = QuestionResponse.class)))
    @ApiResponse(responseCode = "400",
            description = "검증 실패 (VALIDATION_FAILED · OPTIONS_REQUIRED · INVALID_VALUE_RANGE)",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
    @OwnedResourceErrors
    @ApiResponse(responseCode = "409", description = "DRAFT 가 아닌 폼의 질문 편집 (FORM_NOT_EDITABLE)",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping
    public ResponseEntity<QuestionResponse> create(@PathVariable Long formId,
            @Valid @RequestBody QuestionRequest request, Authentication authentication) {
        QuestionResponse created = questionService.create(authentication.getName(), formId, request);
        return ResponseEntity
                .created(URI.create("/api/forms/" + formId + "/questions/" + created.id()))
                .body(created);
    }

    @Operation(summary = "질문 수정", description = "질문 속성과 선택지를 전량 교체합니다.")
    @ApiResponse(responseCode = "200", description = "수정된 질문")
    @ApiResponse(responseCode = "400",
            description = "검증 실패 (VALIDATION_FAILED · OPTIONS_REQUIRED · INVALID_VALUE_RANGE)",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
    @OwnedResourceErrors
    @ApiResponse(responseCode = "409", description = "DRAFT 가 아닌 폼의 질문 편집 (FORM_NOT_EDITABLE)",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
    @PutMapping("/{id}")
    public QuestionResponse update(@PathVariable Long formId, @PathVariable Long id,
            @Valid @RequestBody QuestionRequest request, Authentication authentication) {
        return questionService.update(authentication.getName(), formId, id, request);
    }

    @Operation(summary = "질문 삭제", description = "질문과 하위 선택지를 함께 삭제합니다.")
    @ApiResponse(responseCode = "204", description = "삭제됨 (본문 없음)")
    @OwnedResourceErrors
    @ApiResponse(responseCode = "409", description = "DRAFT 가 아닌 폼의 질문 편집 (FORM_NOT_EDITABLE)",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long formId, @PathVariable Long id, Authentication authentication) {
        questionService.delete(authentication.getName(), formId, id);
    }
}
