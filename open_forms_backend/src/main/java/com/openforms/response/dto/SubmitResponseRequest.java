package com.openforms.response.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 익명 응답 제출 요청입니다. 답하지 않은 선택 질문은 항목 자체를 생략하며, 필수 질문 누락은 서버가
 * 400 {@code REQUIRED_ANSWER_MISSING} 으로 반송합니다.
 */
public record SubmitResponseRequest(@NotEmpty @Valid List<AnswerRequest> answers) {
}
