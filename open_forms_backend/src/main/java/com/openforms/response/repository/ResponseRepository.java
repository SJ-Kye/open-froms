package com.openforms.response.repository;

import com.openforms.response.domain.Response;
import java.sql.Date;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResponseRepository extends JpaRepository<Response, Long> {

    /** 폼의 응답 목록(페이지)입니다. */
    Page<Response> findByForm_Id(Long formId, Pageable pageable);

    /** 폼에 속한 응답을 조회합니다(중첩 리소스 정합 — 다른 폼의 응답이면 빈 값 → 404). */
    Optional<Response> findByIdAndForm_Id(Long id, Long formId);

    /** 폼의 총 응답 수입니다. */
    long countByForm_Id(Long formId);

    /**
     * 일별 응답 수입니다. 응답이 하루도 없던 날은 결과에 나타나지 않으므로, 빈 날을 0 으로 채우는 것은
     * 서비스가 담당합니다 — DB 에 날짜 생성(generate_series 등)을 요구하면 PostgreSQL 전용 문법이 되어
     * H2 테스트가 깨집니다.
     */
    @Query(value = "SELECT CAST(created_at AS DATE) AS day, COUNT(*) AS cnt FROM responses "
            + "WHERE form_id = :formId GROUP BY CAST(created_at AS DATE) ORDER BY day",
            nativeQuery = true)
    List<DailyCountRow> countByDate(@Param("formId") Long formId);

    /**
     * 폼 전체에서 "응답 1건이 질문 1개에 답한" 쌍의 수입니다. 완료율은 이 값을
     * {@code 총 응답 수 × 전체 질문 수} 로 나눈 값이며, 응답마다 응답률을 구해 평균 내는 것과 수학적으로
     * 동일하면서 <b>질의 한 번</b>으로 끝나고 답변 행이 하나도 없는 응답도 0 으로 올바르게 반영됩니다.
     *
     * <p>{@code DISTINCT} 가 필요한 이유는 저장 구조가 "값 하나가 한 행"이라 체크박스 응답이 같은
     * (응답, 질문) 쌍에 여러 행을 만들기 때문입니다. 이를 걸러내지 않으면 완료율이 1.0 을 넘습니다.
     */
    @Query(value = "SELECT COUNT(*) FROM (SELECT DISTINCT a.response_id, a.question_id FROM answers a "
            + "JOIN responses r ON a.response_id = r.id WHERE r.form_id = :formId) t",
            nativeQuery = true)
    long countAnsweredQuestionPairs(@Param("formId") Long formId);

    /**
     * 여러 응답이 각각 답한 질문 수입니다. 목록 화면의 "3/5 문항" 표시를 응답마다 질의하지 않고 현재
     * 페이지 전체를 한 번에 집계합니다(N+1 회피 — 3A 의 responseCount 와 같은 방식).
     */
    @Query(value = "SELECT response_id AS responseId, COUNT(DISTINCT question_id) AS cnt FROM answers "
            + "WHERE response_id IN (:responseIds) GROUP BY response_id", nativeQuery = true)
    List<AnsweredCountRow> countAnsweredQuestionsByResponseIds(
            @Param("responseIds") Collection<Long> responseIds);

    /** 일별 응답 수 집계 결과 투영입니다. */
    interface DailyCountRow {
        Date getDay();

        long getCnt();
    }

    /** 응답별 답변 질문 수 집계 결과 투영입니다. */
    interface AnsweredCountRow {
        Long getResponseId();

        long getCnt();
    }
}
