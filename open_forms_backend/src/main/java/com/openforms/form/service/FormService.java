package com.openforms.form.service;

import com.openforms.common.response.PageResponse;
import com.openforms.form.domain.Form;
import com.openforms.form.domain.FormStatus;
import com.openforms.form.domain.Question;
import com.openforms.form.domain.QuestionOption;
import com.openforms.form.dto.CreateFormRequest;
import com.openforms.form.dto.FormDetailResponse;
import com.openforms.form.dto.FormStatusResponse;
import com.openforms.form.dto.FormSummaryResponse;
import com.openforms.form.dto.OptionResponse;
import com.openforms.form.dto.QuestionResponse;
import com.openforms.form.dto.UpdateFormRequest;
import com.openforms.form.repository.FormRepository;
import com.openforms.form.repository.FormRepository.ResponseCountRow;
import com.openforms.user.domain.User;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
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

    private static final int MAX_SLUG_ATTEMPTS = 10;

    private final FormRepository formRepository;
    private final QuestionLoader questionLoader;
    private final FormAccessGuard accessGuard;
    private final SlugGenerator slugGenerator;

    public FormService(FormRepository formRepository, QuestionLoader questionLoader,
            FormAccessGuard accessGuard, SlugGenerator slugGenerator) {
        this.formRepository = formRepository;
        this.questionLoader = questionLoader;
        this.accessGuard = accessGuard;
        this.slugGenerator = slugGenerator;
    }

    @Transactional
    public FormDetailResponse create(String email, CreateFormRequest request) {
        User owner = accessGuard.currentUser(email);
        Form form = new Form(owner, request.title(), request.description(), uniqueSlug());
        Form saved = formRepository.save(form);
        return FormDetailResponse.of(saved, List.of());
    }

    public PageResponse<FormSummaryResponse> list(String email, FormStatus status, Pageable pageable) {
        UUID userId = accessGuard.currentUser(email).getId();
        Page<Form> page = (status == null)
                ? formRepository.findByUser_Id(userId, pageable)
                : formRepository.findByUser_IdAndStatus(userId, status, pageable);
        Map<Long, Long> counts = responseCounts(page.map(Form::getId).getContent());
        return PageResponse.of(page.map(form ->
                FormSummaryResponse.of(form, counts.getOrDefault(form.getId(), 0L))));
    }

    public FormDetailResponse getDetail(String email, Long id) {
        Form form = accessGuard.requireOwnedForm(id, email);
        return FormDetailResponse.of(form, loadQuestions(form.getId()));
    }

    @Transactional
    public FormDetailResponse update(String email, Long id, UpdateFormRequest request) {
        Form form = accessGuard.requireOwnedForm(id, email);
        form.updateDetails(request.title(), request.description());
        return FormDetailResponse.of(form, loadQuestions(form.getId()));
    }

    @Transactional
    public FormStatusResponse changeStatus(String email, Long id, FormStatus target) {
        Form form = accessGuard.requireOwnedForm(id, email);
        form.changeStatus(target);
        return FormStatusResponse.from(form);
    }

    @Transactional
    public void delete(String email, Long id) {
        Form form = accessGuard.requireOwnedForm(id, email);
        formRepository.delete(form); // 자식(questions·options·responses·answers)은 DB ON DELETE CASCADE 로 정리
    }

    /** 충돌하지 않는 slug 를 확보합니다. 8자 임의값이라 충돌은 드물지만 한정 재시도로 방어합니다. */
    private String uniqueSlug() {
        for (int attempt = 0; attempt < MAX_SLUG_ATTEMPTS; attempt++) {
            String slug = slugGenerator.generate();
            if (formRepository.findBySlug(slug).isEmpty()) {
                return slug;
            }
        }
        throw new IllegalStateException("고유 slug 생성에 반복 실패했습니다.");
    }

    /** 폼별 응답 수를 한 번에 조회해 맵으로 만듭니다. 대상이 없으면 쿼리를 생략합니다. */
    private Map<Long, Long> responseCounts(List<Long> formIds) {
        if (formIds.isEmpty()) {
            return Map.of();
        }
        return formRepository.countResponsesByFormIds(formIds).stream()
                .collect(Collectors.toMap(ResponseCountRow::getFormId, ResponseCountRow::getCnt));
    }

    /** 폼의 질문과 선택지를 position 순으로 조립합니다(로딩·N+1 회피는 {@link QuestionLoader} 가 담당). */
    private List<QuestionResponse> loadQuestions(Long formId) {
        List<Question> questions = questionLoader.questionsOf(formId);
        Map<Long, List<QuestionOption>> optionsByQuestion = questionLoader.optionsByQuestionId(questions);
        return questions.stream()
                .map(q -> QuestionResponse.of(q, optionsByQuestion.getOrDefault(q.getId(), List.of())
                        .stream().map(OptionResponse::from).toList()))
                .toList();
    }
}
