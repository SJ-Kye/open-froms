package com.openforms.form.repository;

import com.openforms.form.domain.Question;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    /** 폼의 질문을 position 오름차순으로 조회합니다(상세 조립용). */
    List<Question> findByForm_IdOrderByPositionAsc(Long formId);
}
