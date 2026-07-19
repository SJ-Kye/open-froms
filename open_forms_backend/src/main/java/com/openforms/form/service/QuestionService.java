package com.openforms.form.service;

import com.openforms.form.dto.QuestionRequest;
import com.openforms.form.dto.QuestionResponse;
import com.openforms.form.repository.QuestionOptionRepository;
import com.openforms.form.repository.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 질문 CRUD 규칙의 단일 지점입니다. 소유권(404→403)은 {@link FormAccessGuard}, 폼 편집 가능 여부(409)는
 * 도메인({@code Form.requireEditable})에 위임하고, 여기서는 타입별 검증·선택지 교체·DTO 변환을 담당합니다.
 */
@Service
@Transactional(readOnly = true)
public class QuestionService {

    private final FormAccessGuard accessGuard;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository optionRepository;

    public QuestionService(FormAccessGuard accessGuard, QuestionRepository questionRepository,
            QuestionOptionRepository optionRepository) {
        this.accessGuard = accessGuard;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
    }

    @Transactional
    public QuestionResponse create(String email, Long formId, QuestionRequest request) {
        throw new UnsupportedOperationException("아직 구현되지 않았습니다.");
    }

    @Transactional
    public QuestionResponse update(String email, Long formId, Long questionId, QuestionRequest request) {
        throw new UnsupportedOperationException("아직 구현되지 않았습니다.");
    }

    @Transactional
    public void delete(String email, Long formId, Long questionId) {
        throw new UnsupportedOperationException("아직 구현되지 않았습니다.");
    }
}
