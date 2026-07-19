package com.openforms.response.service;

import com.openforms.form.service.FormAccessGuard;
import com.openforms.form.service.QuestionLoader;
import com.openforms.response.dto.FormSummaryStats;
import com.openforms.response.repository.AnswerRepository;
import com.openforms.response.repository.ResponseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 대시보드 집계입니다. 계산은 애플리케이션 메모리가 아니라 <b>DB 집계 질의</b>로 수행합니다 — 응답이
 * 늘수록 전 행을 읽어 세는 방식은 비용이 선형으로 커지고, 04 에서 집계 경로에 맞춰 심어 둔 인덱스
 * ({@code ix_responses_form_created}·{@code ix_answers_question_option})가 쓰이지 않습니다.
 */
@Service
@Transactional(readOnly = true)
public class FormSummaryService {

    private final FormAccessGuard formAccessGuard;
    private final QuestionLoader questionLoader;
    private final ResponseRepository responseRepository;
    private final AnswerRepository answerRepository;

    public FormSummaryService(FormAccessGuard formAccessGuard, QuestionLoader questionLoader,
            ResponseRepository responseRepository, AnswerRepository answerRepository) {
        this.formAccessGuard = formAccessGuard;
        this.questionLoader = questionLoader;
        this.responseRepository = responseRepository;
        this.answerRepository = answerRepository;
    }

    /** 폼의 대시보드 집계입니다. */
    public FormSummaryStats summarize(Long formId, String email) {
        throw new UnsupportedOperationException("아직 구현되지 않았습니다.");
    }
}
