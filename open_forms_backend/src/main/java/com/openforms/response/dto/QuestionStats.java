package com.openforms.response.dto;

import com.openforms.form.domain.QuestionType;
import java.util.List;

/**
 * 질문 하나의 응답 분포입니다. 타입마다 의미 있는 통계가 달라 필드를 나눠 담되, <b>목록형 필드는 항상
 * 빈 배열</b>로 채웁니다(null 이 아님) — 화면이 방어 코드 없이 순회할 수 있습니다.
 *
 * <ul>
 *   <li>선택형 → {@code optionCounts} (선택지별 선택 수, 아무도 고르지 않은 선택지도 0 으로 포함)</li>
 *   <li>RATING·NUMBER → {@code average} 와 {@code valueCounts} (값별 개수)</li>
 *   <li>텍스트형 → {@code recentTexts} (최근 응답 일부. 전문은 응답 상세에서 확인)</li>
 *   <li>DATE·TIME → {@code answeredCount} 만</li>
 * </ul>
 *
 * @param answeredCount 이 문항에 답한 응답 수입니다(선택지를 여러 개 고른 응답도 1로 셉니다).
 * @param average       평점·숫자형의 평균입니다. 해당 없거나 응답이 없으면 null 입니다.
 */
public record QuestionStats(Long questionId, QuestionType type, String title, boolean required,
        long answeredCount, Double average, List<OptionCount> optionCounts,
        List<ValueCount> valueCounts, List<String> recentTexts) {

    /** 선택지별 선택 수입니다. */
    public record OptionCount(Long optionId, String label, long count) {
    }

    /** 값별 응답 수입니다(평점 1~5 각각 몇 명인지). */
    public record ValueCount(int value, long count) {
    }
}
