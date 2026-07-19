package com.openforms.form.repository;

import com.openforms.form.domain.QuestionOption;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Long> {

    /** 여러 질문의 선택지를 position 오름차순으로 한 번에 조회합니다(상세 조립 시 N+1 회피). */
    List<QuestionOption> findByQuestion_IdInOrderByPositionAsc(Collection<Long> questionIds);
}
