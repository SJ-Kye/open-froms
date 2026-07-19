package com.openforms.response.controller;

import com.openforms.response.dto.SubmitResponseRequest;
import com.openforms.response.dto.SubmitResponseResult;
import com.openforms.response.service.ResponseSubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 익명 응답 제출 엔드포인트입니다. 인증이 필요 없으므로 {@code @SecurityRequirement} 를 붙이지 않습니다.
 *
 * <p>생성 응답이지만 {@code Location} 헤더는 두지 않습니다. 제출된 응답을 조회하는 경로
 * ({@code /api/forms/{id}/responses/{responseId}})는 <b>제작자 전용</b>이라 익명 제출자가 따라갈 수 없어,
 * 가리킬 수 없는 주소를 주는 대신 접수 결과만 반환합니다.
 */
@Tag(name = "공개 응답", description = "공개 링크(slug)로 응답을 제출합니다 — 인증 불필요")
@RestController
@RequestMapping("/api/public/forms/{slug}/responses")
public class PublicResponseController {

    private final ResponseSubmissionService submissionService;

    public PublicResponseController(ResponseSubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @Operation(summary = "응답 제출",
            description = "발행된 폼에 익명으로 응답합니다. 미발행·없는 slug 는 404, 종료된 폼은 409 입니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubmitResponseResult submit(@PathVariable String slug,
            @Valid @RequestBody SubmitResponseRequest request) {
        return submissionService.submit(slug, request);
    }
}
