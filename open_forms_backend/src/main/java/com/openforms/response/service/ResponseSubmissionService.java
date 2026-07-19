package com.openforms.response.service;

import com.openforms.form.service.PublicFormService;
import com.openforms.form.service.QuestionLoader;
import com.openforms.response.dto.SubmitResponseRequest;
import com.openforms.response.dto.SubmitResponseResult;
import com.openforms.response.repository.AnswerRepository;
import com.openforms.response.repository.ResponseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 익명 응답 제출 규칙의 단일 지점입니다. slug 해석(미발행 404)은 {@link PublicFormService} 에 위임하고,
 * 여기서는 제출 가능 여부(종료 409)와 질문 타입별 값 검증(400)을 담당합니다.
 */
@Service
@Transactional(readOnly = true)
public class ResponseSubmissionService {

    private final PublicFormService publicFormService;
    private final QuestionLoader questionLoader;
    private final ResponseRepository responseRepository;
    private final AnswerRepository answerRepository;

    public ResponseSubmissionService(PublicFormService publicFormService, QuestionLoader questionLoader,
            ResponseRepository responseRepository, AnswerRepository answerRepository) {
        this.publicFormService = publicFormService;
        this.questionLoader = questionLoader;
        this.responseRepository = responseRepository;
        this.answerRepository = answerRepository;
    }

    @Transactional
    public SubmitResponseResult submit(String slug, SubmitResponseRequest request) {
        throw new UnsupportedOperationException("아직 구현되지 않았습니다.");
    }
}
