package com.openforms.response.dto;

import java.time.LocalDateTime;

/**
 * 응답 목록의 항목입니다. 답변 내용은 담지 않고 "언제, 얼마나 답했는가"만 보여 줍니다.
 *
 * <p>{@code answeredCount}/{@code totalQuestions} 를 함께 내려, 목록에서 바로 "3/5 문항 응답"을 표시하고
 * 완성도가 낮은 응답을 골라볼 수 있게 합니다. 답변 원문은 상세 조회의 몫입니다.
 *
 * @param answeredCount  이 응답이 답한 <b>질문 수</b>입니다(체크박스로 선택지를 3개 골라도 1로 셉니다).
 * @param totalQuestions 폼의 전체 질문 수로, 모든 항목이 같은 값을 가집니다.
 */
public record ResponseSummaryItem(Long responseId, LocalDateTime submittedAt,
        long answeredCount, int totalQuestions) {
}
