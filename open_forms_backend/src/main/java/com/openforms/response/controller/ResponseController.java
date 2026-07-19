package com.openforms.response.controller;

import com.openforms.common.openapi.OwnedResourceErrors;
import com.openforms.common.response.PageResponse;
import com.openforms.response.dto.FormSummaryStats;
import com.openforms.response.dto.ResponseDetailResponse;
import com.openforms.response.dto.ResponseSummaryItem;
import com.openforms.response.service.FormSummaryService;
import com.openforms.response.service.ResponseQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 제작자의 응답 조회·삭제와 대시보드 집계입니다. 공개 제출({@code /api/public/**})과 달리 <b>인증과
 * 소유권</b>이 필요하므로 폼 하위 경로에 두고 {@code bearerAuth} 를 표기합니다.
 *
 * <p>수집된 응답을 읽는 경로가 공개 영역에 없다는 점이 중요합니다 — 익명 제출은 쓰기만 가능하고,
 * 읽기는 전부 이 컨트롤러를 통합니다.
 */
@Tag(name = "응답·집계", description = "응답 목록·상세·삭제와 대시보드 집계(제작자 소유)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/forms/{formId}")
public class ResponseController {

    private final ResponseQueryService responseQueryService;
    private final FormSummaryService formSummaryService;

    public ResponseController(ResponseQueryService responseQueryService,
            FormSummaryService formSummaryService) {
        this.responseQueryService = responseQueryService;
        this.formSummaryService = formSummaryService;
    }

    @Operation(summary = "응답 목록", description = "폼의 응답을 최신순으로 조회합니다. 항목마다 답한 문항 수를 포함합니다.")
    @ApiResponse(responseCode = "200", description = "응답 페이지 envelope")
    @OwnedResourceErrors
    @GetMapping("/responses")
    public PageResponse<ResponseSummaryItem> list(@PathVariable Long formId,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable,
            Authentication authentication) {
        return responseQueryService.list(formId, authentication.getName(), pageable);
    }

    @Operation(summary = "응답 상세", description = "문항별 답변을 질문 순서대로 반환합니다(무응답 문항 포함).")
    @ApiResponse(responseCode = "200", description = "문항별 답변")
    @OwnedResourceErrors
    @GetMapping("/responses/{responseId}")
    public ResponseDetailResponse get(@PathVariable Long formId, @PathVariable Long responseId,
            Authentication authentication) {
        return responseQueryService.get(formId, responseId, authentication.getName());
    }

    @Operation(summary = "응답 삭제", description = "응답과 그 답변을 함께 삭제합니다. 되돌릴 수 없습니다.")
    @ApiResponse(responseCode = "204", description = "삭제됨 (본문 없음)")
    @OwnedResourceErrors
    @DeleteMapping("/responses/{responseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long formId, @PathVariable Long responseId,
            Authentication authentication) {
        responseQueryService.delete(formId, responseId, authentication.getName());
    }

    @Operation(summary = "대시보드 집계",
            description = "총 응답·완료율·일별 추이·문항별 분포를 한 번에 반환합니다.")
    @ApiResponse(responseCode = "200", description = "대시보드 집계")
    @OwnedResourceErrors
    @GetMapping("/summary")
    public FormSummaryStats summary(@PathVariable Long formId, Authentication authentication) {
        return formSummaryService.summarize(formId, authentication.getName());
    }
}
