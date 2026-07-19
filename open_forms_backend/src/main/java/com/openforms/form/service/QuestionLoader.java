package com.openforms.form.service;

import com.openforms.form.domain.Question;
import com.openforms.form.domain.QuestionOption;
import com.openforms.form.repository.QuestionOptionRepository;
import com.openforms.form.repository.QuestionRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 폼의 질문과 선택지를 함께 읽어오는 공용 로더입니다. 제작자용 폼 상세({@link FormService})·공개 폼 조회
 * ({@code PublicFormService})·응답 제출 검증({@code ResponseSubmissionService})이 모두 같은 데이터를 필요로 하므로
 * 한곳에 모았습니다.
 *
 * <p>선택지는 질문마다 따로 조회하지 않고 <b>질문 id 목록으로 한 번에</b> 읽어 N+1 을 피합니다.
 * 정렬은 두 조회 모두 {@code position} 오름차순이라, 호출부는 순서를 다시 고민하지 않아도 됩니다.
 */
@Component
@Transactional(readOnly = true)
public class QuestionLoader {

    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository optionRepository;

    public QuestionLoader(QuestionRepository questionRepository,
            QuestionOptionRepository optionRepository) {
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
    }

    /** 폼의 질문을 position 오름차순으로 조회합니다. */
    public List<Question> questionsOf(Long formId) {
        return questionRepository.findByForm_IdOrderByPositionAsc(formId);
    }

    /** 주어진 질문들의 선택지를 질문 id 기준으로 묶어 반환합니다(선택지 없는 질문은 키가 없습니다). */
    public Map<Long, List<QuestionOption>> optionsByQuestionId(List<Question> questions) {
        if (questions.isEmpty()) {
            return Map.of();
        }
        List<Long> questionIds = questions.stream().map(Question::getId).toList();
        return optionRepository.findByQuestion_IdInOrderByPositionAsc(questionIds).stream()
                .collect(Collectors.groupingBy(option -> option.getQuestion().getId()));
    }
}
