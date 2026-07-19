package com.openforms.response.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 응답 1건의 상세입니다. 익명 제출이라 응답자 정보는 없고, 제출 시각과 문항별 답변만 담습니다.
 *
 * <p>{@code answers} 는 <b>폼의 모든 질문</b>을 질문 순서대로 담습니다. 답하지 않은 문항도 빈 값으로
 * 포함시켜, 화면이 폼 구조를 다시 조회하지 않고도 "이 문항은 무응답"을 그릴 수 있게 합니다.
 */
public record ResponseDetailResponse(Long responseId, LocalDateTime submittedAt,
        List<AnswerDetail> answers) {
}
