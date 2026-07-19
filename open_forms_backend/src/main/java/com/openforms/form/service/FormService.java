package com.openforms.form.service;

import com.openforms.common.response.PageResponse;
import com.openforms.form.domain.FormStatus;
import com.openforms.form.dto.CreateFormRequest;
import com.openforms.form.dto.FormDetailResponse;
import com.openforms.form.dto.FormStatusResponse;
import com.openforms.form.dto.FormSummaryResponse;
import com.openforms.form.dto.UpdateFormRequest;
import com.openforms.form.repository.FormRepository;
import com.openforms.form.repository.QuestionOptionRepository;
import com.openforms.form.repository.QuestionRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 폼 CRUD 규칙의 단일 지점입니다. 소유권 검사는 {@link FormAccessGuard}, 상태 전이 불변식은 도메인
 * ({@code Form.changeStatus})에 위임하고, 여기서는 흐름·트랜잭션 경계·DTO 변환을 담당합니다.
 */
@Service
@Transactional(readOnly = true)
public class FormService {

    private final FormRepository formRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository optionRepository;
    private final FormAccessGuard accessGuard;
    private final SlugGenerator slugGenerator;

    public FormService(FormRepository formRepository, QuestionRepository questionRepository,
            QuestionOptionRepository optionRepository, FormAccessGuard accessGuard,
            SlugGenerator slugGenerator) {
        this.formRepository = formRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.accessGuard = accessGuard;
        this.slugGenerator = slugGenerator;
    }

    @Transactional
    public FormDetailResponse create(String email, CreateFormRequest request) {
        throw new UnsupportedOperationException("아직 구현되지 않았습니다.");
    }

    public PageResponse<FormSummaryResponse> list(String email, FormStatus status, Pageable pageable) {
        throw new UnsupportedOperationException("아직 구현되지 않았습니다.");
    }

    public FormDetailResponse getDetail(String email, Long id) {
        throw new UnsupportedOperationException("아직 구현되지 않았습니다.");
    }

    @Transactional
    public FormDetailResponse update(String email, Long id, UpdateFormRequest request) {
        throw new UnsupportedOperationException("아직 구현되지 않았습니다.");
    }

    @Transactional
    public FormStatusResponse changeStatus(String email, Long id, FormStatus target) {
        throw new UnsupportedOperationException("아직 구현되지 않았습니다.");
    }

    @Transactional
    public void delete(String email, Long id) {
        throw new UnsupportedOperationException("아직 구현되지 않았습니다.");
    }
}
