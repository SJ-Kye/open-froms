package com.openforms.form.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.openforms.common.exception.BadRequestException;
import com.openforms.common.exception.ConflictException;
import com.openforms.common.exception.ResourceNotFoundException;
import com.openforms.form.domain.Form;
import com.openforms.form.domain.FormStatus;
import com.openforms.form.domain.Question;
import com.openforms.form.domain.QuestionOption;
import com.openforms.form.domain.QuestionType;
import com.openforms.form.dto.OptionRequest;
import com.openforms.form.dto.QuestionRequest;
import com.openforms.form.dto.QuestionResponse;
import com.openforms.form.repository.QuestionOptionRepository;
import com.openforms.form.repository.QuestionRepository;
import com.openforms.user.domain.User;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 질문 서비스 규칙을 하이브리드 TDD 로 먼저 고정합니다: 선택형 선택지 ≥2, RATING/NUMBER 범위 검증,
 * 발행/종료 폼 편집 금지(409), 중첩 리소스 정합(404), 비선택형 옵션 무시, 수정 시 선택지 전량 교체.
 */
@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    @Mock
    private FormAccessGuard accessGuard;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private QuestionOptionRepository optionRepository;
    @InjectMocks
    private QuestionService questionService;

    private static final String EMAIL = "owner@example.com";

    private Form draftForm() {
        return new Form(mock(User.class), "폼", "설명", "slug1234"); // 생성자 → DRAFT
    }

    @Test
    @DisplayName("선택형인데 선택지가 2개 미만이면 400(OPTIONS_REQUIRED)")
    void choiceRequiresAtLeastTwoOptions() {
        when(accessGuard.requireOwnedForm(1L, EMAIL)).thenReturn(draftForm());
        QuestionRequest request = new QuestionRequest(QuestionType.SINGLE_CHOICE, "질문", true, 1,
                null, null, List.of(new OptionRequest("유일", 1)));

        assertThatThrownBy(() -> questionService.create(EMAIL, 1L, request))
                .asInstanceOf(InstanceOfAssertFactories.type(BadRequestException.class))
                .satisfies(ex -> assertThat(ex.getCode()).isEqualTo("OPTIONS_REQUIRED"));
    }

    @Test
    @DisplayName("RATING 의 minValue > maxValue 면 400(INVALID_VALUE_RANGE)")
    void ratingRejectsInvertedRange() {
        when(accessGuard.requireOwnedForm(1L, EMAIL)).thenReturn(draftForm());
        QuestionRequest request = new QuestionRequest(QuestionType.RATING, "점수", true, 1, 5, 1, null);

        assertThatThrownBy(() -> questionService.create(EMAIL, 1L, request))
                .asInstanceOf(InstanceOfAssertFactories.type(BadRequestException.class))
                .satisfies(ex -> assertThat(ex.getCode()).isEqualTo("INVALID_VALUE_RANGE"));
    }

    @Test
    @DisplayName("발행/종료된 폼의 질문 편집은 409(FORM_NOT_EDITABLE)")
    void rejectsEditingNonDraftForm() {
        Form published = draftForm();
        published.changeStatus(FormStatus.PUBLISHED);
        when(accessGuard.requireOwnedForm(1L, EMAIL)).thenReturn(published);
        QuestionRequest request = new QuestionRequest(QuestionType.SHORT_TEXT, "질문", true, 1, null, null, null);

        assertThatThrownBy(() -> questionService.create(EMAIL, 1L, request))
                .asInstanceOf(InstanceOfAssertFactories.type(ConflictException.class))
                .satisfies(ex -> assertThat(ex.getCode()).isEqualTo("FORM_NOT_EDITABLE"));
    }

    @Test
    @DisplayName("폼에 속하지 않은 질문 수정은 404(QUESTION_NOT_FOUND)")
    void updateMissingQuestion() {
        when(accessGuard.requireOwnedForm(1L, EMAIL)).thenReturn(draftForm());
        when(questionRepository.findByIdAndForm_Id(99L, 1L)).thenReturn(Optional.empty());
        QuestionRequest request = new QuestionRequest(QuestionType.SHORT_TEXT, "질문", true, 1, null, null, null);

        assertThatThrownBy(() -> questionService.update(EMAIL, 1L, 99L, request))
                .asInstanceOf(InstanceOfAssertFactories.type(ResourceNotFoundException.class))
                .satisfies(ex -> assertThat(ex.getCode()).isEqualTo("QUESTION_NOT_FOUND"));
    }

    @Test
    @DisplayName("비선택형(SHORT_TEXT)은 옵션·범위를 무시하고 min/max=null 로 저장한다")
    void shortTextIgnoresOptionsAndRange() {
        when(accessGuard.requireOwnedForm(1L, EMAIL)).thenReturn(draftForm());
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));
        // 클라이언트가 옵션·범위를 잘못 보내더라도 타입에 맞지 않으면 저장하지 않는다.
        QuestionRequest request = new QuestionRequest(QuestionType.SHORT_TEXT, "의견", false, 1, 1, 5,
                List.of(new OptionRequest("무시됨", 1)));

        QuestionResponse response = questionService.create(EMAIL, 1L, request);

        ArgumentCaptor<Question> saved = ArgumentCaptor.forClass(Question.class);
        verify(questionRepository).save(saved.capture());
        assertThat(saved.getValue().getMinValue()).isNull();
        assertThat(saved.getValue().getMaxValue()).isNull();
        verify(optionRepository, never()).saveAll(any());
        assertThat(response.options()).isEmpty();
    }

    @Test
    @DisplayName("선택형(SINGLE_CHOICE)은 선택지를 저장한다")
    void choicePersistsOptions() {
        when(accessGuard.requireOwnedForm(1L, EMAIL)).thenReturn(draftForm());
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));
        when(optionRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        QuestionRequest request = new QuestionRequest(QuestionType.SINGLE_CHOICE, "만족도", true, 1, null, null,
                List.of(new OptionRequest("만족", 1), new OptionRequest("불만족", 2)));

        QuestionResponse response = questionService.create(EMAIL, 1L, request);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<QuestionOption>> saved = ArgumentCaptor.forClass(List.class);
        verify(optionRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).hasSize(2)
                .extracting(QuestionOption::getLabel).containsExactly("만족", "불만족");
        assertThat(response.options()).hasSize(2);
    }

    @Test
    @DisplayName("질문 수정은 기존 선택지를 지우고 새 선택지로 교체한다")
    void updateReplacesOptions() {
        Form form = draftForm();
        Question existing = new Question(form, QuestionType.SINGLE_CHOICE, "이전", true, 1, null, null);
        when(accessGuard.requireOwnedForm(1L, EMAIL)).thenReturn(form);
        when(questionRepository.findByIdAndForm_Id(100L, 1L)).thenReturn(Optional.of(existing));
        when(optionRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        QuestionRequest request = new QuestionRequest(QuestionType.SINGLE_CHOICE, "바뀐 질문", true, 1, null, null,
                List.of(new OptionRequest("A", 1), new OptionRequest("B", 2)));

        QuestionResponse response = questionService.update(EMAIL, 1L, 100L, request);

        verify(optionRepository).deleteByQuestion_Id(100L);
        verify(optionRepository).saveAll(any());
        assertThat(existing.getTitle()).isEqualTo("바뀐 질문");
        assertThat(response.title()).isEqualTo("바뀐 질문");
        assertThat(response.options()).hasSize(2);
    }
}
