package com.openforms.form.repository;

import com.openforms.form.domain.Question;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    /** 폼의 질문을 position 오름차순으로 조회합니다(상세 조립용). */
    List<Question> findByForm_IdOrderByPositionAsc(Long formId);

    /** 특정 폼에 속한 질문을 조회합니다(중첩 리소스 정합 — 다른 폼의 질문이면 빈 값 → 404). */
    Optional<Question> findByIdAndForm_Id(Long id, Long formId);
}
