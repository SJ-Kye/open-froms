package com.openforms.form.service;

import com.openforms.common.exception.ResourceNotFoundException;
import com.openforms.form.domain.Form;
import com.openforms.form.domain.FormStatus;
import com.openforms.form.domain.Question;
import com.openforms.form.domain.QuestionOption;
import com.openforms.form.dto.PublicFormResponse;
import com.openforms.form.dto.PublicOptionResponse;
import com.openforms.form.dto.PublicQuestionResponse;
import com.openforms.form.repository.FormRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 공개 링크(slug)로 폼을 여는 경로입니다. 인증이 없으므로 소유권 검사 대신 <b>발행 여부</b>가 접근 기준이 됩니다.
 *
 * <p>아직 발행되지 않은(DRAFT) 폼은 "없음"과 <b>같은 404</b> 로 답합니다. 미발행을 403 등으로 구분하면
 * slug 를 넣어보는 것만으로 준비 중인 폼의 존재를 알 수 있기 때문입니다. 종료(CLOSED)된 폼은 조회는
 * 허용하고 제출만 막아, 응답 화면이 "종료된 설문입니다"를 안내할 수 있게 합니다.
 */
@Service
@Transactional(readOnly = true)
public class PublicFormService {

    private final FormRepository formRepository;
    private final QuestionLoader questionLoader;

    public PublicFormService(FormRepository formRepository, QuestionLoader questionLoader) {
        this.formRepository = formRepository;
        this.questionLoader = questionLoader;
    }

    public PublicFormResponse get(String slug) {
        Form form = requireVisibleForm(slug);
        List<Question> questions = questionLoader.questionsOf(form.getId());
        Map<Long, List<QuestionOption>> optionsByQuestion = questionLoader.optionsByQuestionId(questions);
        return PublicFormResponse.of(form, questions.stream()
                .map(q -> PublicQuestionResponse.of(q, optionsByQuestion.getOrDefault(q.getId(), List.of())
                        .stream().map(PublicOptionResponse::from).toList()))
                .toList());
    }

    /**
     * 공개적으로 볼 수 있는 폼을 반환합니다(발행되었거나 종료된 폼). 없거나 아직 발행 전이면 404 입니다.
     * 응답 제출도 같은 기준으로 slug 를 해석하므로 이 메서드를 공유합니다.
     */
    public Form requireVisibleForm(String slug) {
        return formRepository.findBySlug(slug)
                .filter(form -> form.getStatus() != FormStatus.DRAFT)
                .orElseThrow(() -> new ResourceNotFoundException("FORM_NOT_FOUND", "폼을 찾을 수 없습니다."));
    }
}
