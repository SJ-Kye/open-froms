package com.openforms.response.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 대시보드 집계 결과입니다. 카드(총 응답·완료율)·라인 차트(일별 추이)·문항별 차트에 필요한 데이터를
 * 한 번의 요청으로 모두 내려, 화면이 폼 구조와 응답을 따로 조회해 맞추지 않게 합니다.
 *
 * @param completionRate 응답 1건당 <b>답한 질문 수 / 전체 질문 수</b> 의 평균(0.0~1.0)입니다. 필수 문항만
 *                       보는 대신 전체 문항 대비 응답률을 쓰므로, 선택 문항을 많이 건너뛴 응답이 그대로
 *                       드러납니다. 질문이 없거나 응답이 없으면 0.0 입니다.
 * @param responsesByDate 발행일부터 종료일(종료 전이면 오늘)까지 <b>빠짐없이</b> 이어지는 일별 응답 수입니다.
 */
public record FormSummaryStats(Long formId, long totalResponses, double completionRate,
        List<DailyCount> responsesByDate, List<QuestionStats> questionSummaries) {

    /** 하루치 응답 수입니다. 응답이 없던 날도 0 으로 포함됩니다. */
    public record DailyCount(LocalDate date, long count) {
    }
}
