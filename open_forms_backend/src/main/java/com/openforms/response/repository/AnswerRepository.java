package com.openforms.response.repository;

import com.openforms.response.domain.Answer;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    /** 응답 1건의 답변 행을 모두 조회합니다(상세 조립용 — 서비스가 질문 기준으로 다시 묶습니다). */
    List<Answer> findByResponse_Id(Long responseId);

    /**
     * 문항별로 답한 응답 수입니다. 선택지를 여러 개 고른 응답이 여러 행이 되므로 {@code DISTINCT} 로
     * 응답 단위로 셉니다.
     */
    @Query(value = "SELECT a.question_id AS questionId, COUNT(DISTINCT a.response_id) AS cnt FROM answers a "
            + "JOIN responses r ON a.response_id = r.id WHERE r.form_id = :formId GROUP BY a.question_id",
            nativeQuery = true)
    List<QuestionCountRow> countRespondentsByQuestion(@Param("formId") Long formId);

    /**
     * 선택지별 선택 수입니다. 04 의 복합 인덱스 {@code ix_answers_question_option} 이 그대로 쓰이는
     * 질의로, 대시보드의 핵심 차트를 조인 없이 얻기 위해 저장 구조를 그렇게 잡았습니다.
     */
    @Query(value = "SELECT a.question_id AS questionId, a.option_id AS optionId, COUNT(*) AS cnt FROM answers a "
            + "JOIN responses r ON a.response_id = r.id "
            + "WHERE r.form_id = :formId AND a.option_id IS NOT NULL "
            + "GROUP BY a.question_id, a.option_id", nativeQuery = true)
    List<OptionCountRow> countByOption(@Param("formId") Long formId);

    /**
     * 평점·숫자형의 값별 응답 수입니다. 평균은 이 결과로부터 계산할 수 있으므로(값×개수의 합 ÷ 개수의 합)
     * 평균을 위한 질의를 따로 두지 않습니다.
     */
    @Query(value = "SELECT a.question_id AS questionId, a.number_value AS value, COUNT(*) AS cnt FROM answers a "
            + "JOIN responses r ON a.response_id = r.id "
            + "WHERE r.form_id = :formId AND a.number_value IS NOT NULL "
            + "GROUP BY a.question_id, a.number_value ORDER BY a.question_id, a.number_value",
            nativeQuery = true)
    List<ValueCountRow> countByNumberValue(@Param("formId") Long formId);

    /**
     * 한 문항의 최근 텍스트 응답입니다. 개수 제한은 {@link Pageable} 로 걸어 DB 별 LIMIT 문법 차이를
     * 피합니다. 텍스트형 문항 수만큼 호출되지만 그 수는 폼당 한 자릿수라 문제되지 않습니다.
     */
    @Query("SELECT a.textValue FROM Answer a WHERE a.question.id = :questionId "
            + "AND a.textValue IS NOT NULL ORDER BY a.id DESC")
    List<String> findRecentTexts(@Param("questionId") Long questionId, Pageable pageable);

    /** 문항별 응답자 수 투영입니다. */
    interface QuestionCountRow {
        Long getQuestionId();

        long getCnt();
    }

    /** 선택지별 선택 수 투영입니다. */
    interface OptionCountRow {
        Long getQuestionId();

        Long getOptionId();

        long getCnt();
    }

    /** 값별 응답 수 투영입니다. */
    interface ValueCountRow {
        Long getQuestionId();

        int getValue();

        long getCnt();
    }
}
