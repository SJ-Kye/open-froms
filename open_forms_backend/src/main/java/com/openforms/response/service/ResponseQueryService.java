package com.openforms.response.service;

import com.openforms.common.response.PageResponse;
import com.openforms.form.service.FormAccessGuard;
import com.openforms.form.service.QuestionLoader;
import com.openforms.response.dto.ResponseDetailResponse;
import com.openforms.response.dto.ResponseSummaryItem;
import com.openforms.response.repository.AnswerRepository;
import com.openforms.response.repository.ResponseRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 제작자의 응답 조회·삭제입니다. 소유권 검증은 {@link FormAccessGuard} 에 위임하여 폼·질문 API 와 같은
 * 규칙(존재 404 → 소유 403)을 그대로 따릅니다.
 */
@Service
@Transactional(readOnly = true)
public class ResponseQueryService {

    private final FormAccessGuard formAccessGuard;
    private final QuestionLoader questionLoader;
    private final ResponseRepository responseRepository;
    private final AnswerRepository answerRepository;

    public ResponseQueryService(FormAccessGuard formAccessGuard, QuestionLoader questionLoader,
            ResponseRepository responseRepository, AnswerRepository answerRepository) {
        this.formAccessGuard = formAccessGuard;
        this.questionLoader = questionLoader;
        this.responseRepository = responseRepository;
        this.answerRepository = answerRepository;
    }

    /** 폼의 응답 목록입니다. */
    public PageResponse<ResponseSummaryItem> list(Long formId, String email, Pageable pageable) {
        throw new UnsupportedOperationException("아직 구현되지 않았습니다.");
    }

    /** 응답 1건의 상세입니다. */
    public ResponseDetailResponse get(Long formId, Long responseId, String email) {
        throw new UnsupportedOperationException("아직 구현되지 않았습니다.");
    }

    /** 응답 1건을 삭제합니다. */
    @Transactional
    public void delete(Long formId, Long responseId, String email) {
        throw new UnsupportedOperationException("아직 구현되지 않았습니다.");
    }
}
