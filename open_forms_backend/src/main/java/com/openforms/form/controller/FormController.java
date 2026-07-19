package com.openforms.form.controller;

import com.openforms.common.exception.ErrorResponse;
import com.openforms.common.openapi.AuthenticatedErrors;
import com.openforms.common.openapi.OwnedResourceErrors;
import com.openforms.common.openapi.ValidationErrors;
import com.openforms.common.response.PageResponse;
import com.openforms.form.domain.FormStatus;
import com.openforms.form.dto.CreateFormRequest;
import com.openforms.form.dto.FormDetailResponse;
import com.openforms.form.dto.FormStatusResponse;
import com.openforms.form.dto.FormSummaryResponse;
import com.openforms.form.dto.UpdateFormRequest;
import com.openforms.form.dto.UpdateFormStatusRequest;
import com.openforms.form.service.FormService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 폼 CRUD 엔드포인트입니다. 모두 인증·소유자 전용이며, HTTP 매핑·검증·상태 코드만 담당하고 규칙은
 * {@link FormService} 에 위임합니다. principal(이메일)은 JWT 필터가 주입한 {@link Authentication} 에서 얻습니다.
 */
@Tag(name = "폼", description = "폼 생성·목록·조회·수정·상태 변경·삭제(제작자 소유)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/forms")
public class FormController {

    private final FormService formService;

    public FormController(FormService formService) {
        this.formService = formService;
    }

    @Operation(summary = "폼 목록", description = "내 폼을 페이지네이션·상태 필터·정렬로 조회합니다.")
    @ApiResponse(responseCode = "200", description = "페이지 envelope")
    @AuthenticatedErrors
    @GetMapping
    public PageResponse<FormSummaryResponse> list(
            @RequestParam(required = false) FormStatus status,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable,
            Authentication authentication) {
        return formService.list(authentication.getName(), status, pageable);
    }

    @Operation(summary = "폼 생성", description = "DRAFT 상태로 폼을 만들고 slug 를 발급합니다.")
    // ResponseEntity 로 201 을 내는 오퍼레이션은 반환 타입만으로 상태 코드를 알 수 없어 springdoc 이 200 으로
    // 추론합니다. 실제 계약(201 + Location)을 스펙에 드러내기 위해 성공 응답을 직접 적습니다.
    @ApiResponse(responseCode = "201", description = "생성됨 (Location 헤더에 상세 경로)",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = FormDetailResponse.class)))
    @ValidationErrors
    @AuthenticatedErrors
    @PostMapping
    public ResponseEntity<FormDetailResponse> create(@Valid @RequestBody CreateFormRequest request,
            Authentication authentication) {
        FormDetailResponse created = formService.create(authentication.getName(), request);
        return ResponseEntity.created(URI.create("/api/forms/" + created.id())).body(created);
    }

    @Operation(summary = "폼 상세", description = "질문·선택지를 포함한 폼 상세를 반환합니다.")
    @ApiResponse(responseCode = "200", description = "폼 상세")
    @OwnedResourceErrors
    @GetMapping("/{id}")
    public FormDetailResponse get(@PathVariable Long id, Authentication authentication) {
        return formService.getDetail(authentication.getName(), id);
    }

    @Operation(summary = "폼 수정", description = "제목·설명을 변경합니다.")
    @ApiResponse(responseCode = "200", description = "수정된 폼 상세")
    @ValidationErrors
    @OwnedResourceErrors
    @PutMapping("/{id}")
    public FormDetailResponse update(@PathVariable Long id, @Valid @RequestBody UpdateFormRequest request,
            Authentication authentication) {
        return formService.update(authentication.getName(), id, request);
    }

    @Operation(summary = "폼 상태 변경", description = "DRAFT→PUBLISHED→CLOSED 선형 전이. 무효 전이는 409.")
    @ApiResponse(responseCode = "200", description = "전이된 상태")
    @ValidationErrors
    @OwnedResourceErrors
    @ApiResponse(responseCode = "409", description = "허용되지 않는 상태 전이 (INVALID_STATUS_TRANSITION)",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
    @PatchMapping("/{id}/status")
    public FormStatusResponse changeStatus(@PathVariable Long id,
            @Valid @RequestBody UpdateFormStatusRequest request, Authentication authentication) {
        return formService.changeStatus(authentication.getName(), id, request.status());
    }

    @Operation(summary = "폼 삭제", description = "폼과 하위 질문·선택지·응답이 함께 삭제됩니다.")
    @ApiResponse(responseCode = "204", description = "삭제됨 (본문 없음)")
    @OwnedResourceErrors
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        formService.delete(authentication.getName(), id);
    }
}
