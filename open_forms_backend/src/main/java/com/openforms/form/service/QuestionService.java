package com.openforms.form.service;

import com.openforms.common.exception.BadRequestException;
import com.openforms.common.exception.ResourceNotFoundException;
import com.openforms.form.domain.Form;
import com.openforms.form.domain.Question;
import com.openforms.form.domain.QuestionOption;
import com.openforms.form.dto.OptionRequest;
import com.openforms.form.dto.OptionResponse;
import com.openforms.form.dto.QuestionRequest;
import com.openforms.form.dto.QuestionResponse;
import com.openforms.form.repository.QuestionOptionRepository;
import com.openforms.form.repository.QuestionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 질문 CRUD 규칙의 단일 지점입니다. 소유권(404→403)은 {@link FormAccessGuard}, 폼 편집 가능 여부(409)는
 * 도메인({@code Form.requireEditable})에 위임하고, 여기서는 타입별 검증·선택지 교체·DTO 변환을 담당합니다.
 *
 * <p>선택지는 값이 아니라 별도 엔티티라, 수정 시 부분 병합 대신 <b>전량 교체</b>(기존 삭제 후 재삽입)로
 * 단순·일관하게 다룹니다. 최소 선택지 수·범위 검증은 타입에 따라 달라져 애너테이션으로 표현하기 어렵기에
 * 여기서 {@link BadRequestException} 으로 반송합니다.
 */
@Service
@Transactional(readOnly = true)
public class QuestionService {

    private static final int MIN_CHOICE_OPTIONS = 2;

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
        Form form = accessGuard.requireOwnedForm(formId, email);
        form.requireEditable();
        validate(request);

        Question question = questionRepository.save(new Question(form, request.type(), request.title(),
                request.required(), request.position(), rangeMin(request), rangeMax(request)));
        return QuestionResponse.of(question, saveOptions(question, request));
    }

    @Transactional
    public QuestionResponse update(String email, Long formId, Long questionId, QuestionRequest request) {
        Form form = accessGuard.requireOwnedForm(formId, email);
        form.requireEditable();
        Question question = questionRepository.findByIdAndForm_Id(questionId, formId)
                .orElseThrow(() -> new ResourceNotFoundException("QUESTION_NOT_FOUND", "질문을 찾을 수 없습니다."));
        validate(request);

        question.update(request.type(), request.title(), request.required(), request.position(),
                rangeMin(request), rangeMax(request));
        optionRepository.deleteByQuestion_Id(questionId); // 선택지 전량 교체
        return QuestionResponse.of(question, saveOptions(question, request));
    }

    @Transactional
    public void delete(String email, Long formId, Long questionId) {
        Form form = accessGuard.requireOwnedForm(formId, email);
        form.requireEditable();
        Question question = questionRepository.findByIdAndForm_Id(questionId, formId)
                .orElseThrow(() -> new ResourceNotFoundException("QUESTION_NOT_FOUND", "질문을 찾을 수 없습니다."));
        questionRepository.delete(question); // 선택지·answers 는 DB ON DELETE CASCADE 로 정리
    }

    /** 타입에 따라 달라지는 교차 규칙을 검증합니다(선택형→선택지 ≥2, RATING/NUMBER→min≤max). */
    private void validate(QuestionRequest request) {
        if (request.type().isChoice() && optionCount(request) < MIN_CHOICE_OPTIONS) {
            throw new BadRequestException("OPTIONS_REQUIRED",
                    "선택형 질문에는 선택지가 " + MIN_CHOICE_OPTIONS + "개 이상 필요합니다.");
        }
        if (request.type().hasRange() && request.minValue() != null && request.maxValue() != null
                && request.minValue() > request.maxValue()) {
            throw new BadRequestException("INVALID_VALUE_RANGE",
                    "minValue 는 maxValue 보다 클 수 없습니다.");
        }
    }

    /** 선택형이면 요청 선택지를 저장해 응답으로, 그 외 타입이면 선택지 없이 빈 목록을 돌려줍니다. */
    private List<OptionResponse> saveOptions(Question question, QuestionRequest request) {
        if (!request.type().isChoice()) {
            return List.of();
        }
        List<QuestionOption> options = request.options().stream()
                .map(o -> new QuestionOption(question, o.label(), o.position()))
                .toList();
        return optionRepository.saveAll(options).stream().map(OptionResponse::from).toList();
    }

    private int optionCount(QuestionRequest request) {
        return request.options() == null ? 0 : request.options().size();
    }

    /** 범위 메타는 RATING/NUMBER 에서만 의미가 있으므로 그 외 타입에서는 null 로 정규화합니다. */
    private Integer rangeMin(QuestionRequest request) {
        return request.type().hasRange() ? request.minValue() : null;
    }

    private Integer rangeMax(QuestionRequest request) {
        return request.type().hasRange() ? request.maxValue() : null;
    }
}
