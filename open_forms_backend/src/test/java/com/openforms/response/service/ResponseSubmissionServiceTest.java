package com.openforms.response.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.openforms.common.exception.BadRequestException;
import com.openforms.common.exception.ConflictException;
import com.openforms.form.domain.Form;
import com.openforms.form.domain.FormStatus;
import com.openforms.form.domain.Question;
import com.openforms.form.domain.QuestionOption;
import com.openforms.form.domain.QuestionType;
import com.openforms.form.service.PublicFormService;
import com.openforms.form.service.QuestionLoader;
import com.openforms.response.domain.Answer;
import com.openforms.response.domain.Response;
import com.openforms.response.dto.AnswerRequest;
import com.openforms.response.dto.SubmitResponseRequest;
import com.openforms.response.dto.SubmitResponseResult;
import com.openforms.response.repository.AnswerRepository;
import com.openforms.response.repository.ResponseRepository;
import com.openforms.user.domain.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 응답 제출 규칙을 하이브리드 TDD 로 먼저 고정합니다: 종료된 폼 409, 필수 미응답 400(+fieldErrors),
 * 폼 밖 질문·중복 제출·타입 불일치·범위 위반 400, 그리고 선택형이 고른 개수만큼 행으로 저장되는 것.
 */
@ExtendWith(MockitoExtension.class)
class ResponseSubmissionServiceTest {

    @Mock
    private PublicFormService publicFormService;
    @Mock
    private QuestionLoader questionLoader;
    @Mock
    private ResponseRepository responseRepository;
    @Mock
    private AnswerRepository answerRepository;
    @InjectMocks
    private ResponseSubmissionService service;

    private static final String SLUG = "slug1234";

    @Test
    @DisplayName("종료된 폼에는 응답할 수 없다 → 409(FORM_CLOSED)")
    void rejectsClosedForm() {
        when(publicFormService.requireVisibleForm(SLUG)).thenReturn(form(FormStatus.CLOSED));

        assertThatThrownBy(() -> service.submit(SLUG, request(new AnswerRequest(1L, null, "답", null, null, null))))
                .asInstanceOf(InstanceOfAssertFactories.type(ConflictException.class))
                .satisfies(ex -> assertThat(ex.getCode()).isEqualTo("FORM_CLOSED"));
    }

    @Test
    @DisplayName("필수 질문 미응답 → 400(REQUIRED_ANSWER_MISSING) + 빠진 질문이 fieldErrors 에 담긴다")
    void rejectsMissingRequiredAnswer() {
        Form form = form(FormStatus.PUBLISHED);
        Question required = question(form, 100L, QuestionType.SHORT_TEXT, true, null, null);
        Question optional = question(form, 101L, QuestionType.SHORT_TEXT, false, null, null);
        stubQuestions(form, List.of(required, optional), Map.of());

        // 선택 질문만 답하고 필수 질문(100)은 항목 자체를 생략
        assertThatThrownBy(() -> service.submit(SLUG,
                request(new AnswerRequest(101L, null, "선택 답변", null, null, null))))
                .asInstanceOf(InstanceOfAssertFactories.type(BadRequestException.class))
                .satisfies(ex -> {
                    assertThat(ex.getCode()).isEqualTo("REQUIRED_ANSWER_MISSING");
                    assertThat(ex.getFieldErrors()).singleElement()
                            .satisfies(fe -> assertThat(fe.field()).isEqualTo("questionId:100"));
                });
    }

    @Test
    @DisplayName("폼에 없는 질문에 응답 → 400(UNKNOWN_QUESTION)")
    void rejectsUnknownQuestion() {
        Form form = form(FormStatus.PUBLISHED);
        stubQuestions(form, List.of(question(form, 100L, QuestionType.SHORT_TEXT, false, null, null)), Map.of());

        assertThatThrownBy(() -> service.submit(SLUG, request(new AnswerRequest(999L, null, "답", null, null, null))))
                .asInstanceOf(InstanceOfAssertFactories.type(BadRequestException.class))
                .satisfies(ex -> assertThat(ex.getCode()).isEqualTo("UNKNOWN_QUESTION"));
    }

    @Test
    @DisplayName("같은 질문을 두 번 제출 → 400(DUPLICATE_ANSWER)")
    void rejectsDuplicateAnswer() {
        Form form = form(FormStatus.PUBLISHED);
        stubQuestions(form, List.of(question(form, 100L, QuestionType.SHORT_TEXT, false, null, null)), Map.of());

        assertThatThrownBy(() -> service.submit(SLUG,
                request(new AnswerRequest(100L, null, "첫번째", null, null, null),
                        new AnswerRequest(100L, null, "두번째", null, null, null))))
                .asInstanceOf(InstanceOfAssertFactories.type(BadRequestException.class))
                .satisfies(ex -> assertThat(ex.getCode()).isEqualTo("DUPLICATE_ANSWER"));
    }

    @Test
    @DisplayName("택1 질문에 선택지를 2개 고르면 → 400(INVALID_ANSWER_VALUE)")
    void rejectsMultipleOptionsOnSingleChoice() {
        Form form = form(FormStatus.PUBLISHED);
        Question single = question(form, 100L, QuestionType.SINGLE_CHOICE, false, null, null);
        stubQuestions(form, List.of(single),
                Map.of(100L, List.of(option(1000L, single, "A"), option(1001L, single, "B"))));

        assertThatThrownBy(() -> service.submit(SLUG,
                request(new AnswerRequest(100L, List.of(1000L, 1001L), null, null, null, null))))
                .asInstanceOf(InstanceOfAssertFactories.type(BadRequestException.class))
                .satisfies(ex -> assertThat(ex.getCode()).isEqualTo("INVALID_ANSWER_VALUE"));
    }

    @Test
    @DisplayName("그 질문의 것이 아닌 선택지 id → 400(INVALID_ANSWER_VALUE)")
    void rejectsForeignOption() {
        Form form = form(FormStatus.PUBLISHED);
        Question choice = question(form, 100L, QuestionType.SINGLE_CHOICE, false, null, null);
        stubQuestions(form, List.of(choice), Map.of(100L, List.of(option(1000L, choice, "A"))));

        assertThatThrownBy(() -> service.submit(SLUG,
                request(new AnswerRequest(100L, List.of(9999L), null, null, null, null))))
                .asInstanceOf(InstanceOfAssertFactories.type(BadRequestException.class))
                .satisfies(ex -> assertThat(ex.getCode()).isEqualTo("INVALID_ANSWER_VALUE"));
    }

    @Test
    @DisplayName("RATING 이 허용 범위를 벗어나면 → 400(ANSWER_OUT_OF_RANGE)")
    void rejectsOutOfRangeRating() {
        Form form = form(FormStatus.PUBLISHED);
        stubQuestions(form, List.of(question(form, 100L, QuestionType.RATING, false, 1, 5)), Map.of());

        assertThatThrownBy(() -> service.submit(SLUG,
                request(new AnswerRequest(100L, null, null, 9, null, null))))
                .asInstanceOf(InstanceOfAssertFactories.type(BadRequestException.class))
                .satisfies(ex -> assertThat(ex.getCode()).isEqualTo("ANSWER_OUT_OF_RANGE"));
    }

    @Test
    @DisplayName("정상 제출 — 체크박스는 고른 수만큼, 그 외는 1행으로 저장된다")
    void savesOneRowPerSelectedOption() {
        Form form = form(FormStatus.PUBLISHED);
        Question text = question(form, 100L, QuestionType.SHORT_TEXT, true, null, null);
        Question multi = question(form, 101L, QuestionType.MULTIPLE_CHOICE, true, null, null);
        stubQuestions(form, List.of(text, multi),
                Map.of(101L, List.of(option(1010L, multi, "가"), option(1011L, multi, "나"),
                        option(1012L, multi, "다"))));
        when(responseRepository.save(any(Response.class))).thenAnswer(inv -> {
            Response saved = inv.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 500L);
            ReflectionTestUtils.setField(saved, "createdAt", LocalDateTime.now());
            return saved;
        });

        SubmitResponseResult result = service.submit(SLUG,
                request(new AnswerRequest(100L, null, "친절했습니다", null, null, null),
                        new AnswerRequest(101L, List.of(1010L, 1012L), null, null, null, null)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Answer>> saved = ArgumentCaptor.forClass(List.class);
        verify(answerRepository).saveAll(saved.capture());
        // 단답 1행 + 체크박스 2행 = 3행
        assertThat(saved.getValue()).hasSize(3);
        assertThat(saved.getValue()).filteredOn(a -> a.getOption() != null)
                .extracting(a -> a.getOption().getId()).containsExactly(1010L, 1012L);
        assertThat(result.responseId()).isEqualTo(500L);
        assertThat(result.submittedAt()).isNotNull();
    }

    // --- fixtures ---

    private void stubQuestions(Form form, List<Question> questions, Map<Long, List<QuestionOption>> options) {
        when(publicFormService.requireVisibleForm(SLUG)).thenReturn(form);
        when(questionLoader.questionsOf(form.getId())).thenReturn(questions);
        when(questionLoader.optionsByQuestionId(questions)).thenReturn(options);
    }

    private SubmitResponseRequest request(AnswerRequest... answers) {
        return new SubmitResponseRequest(List.of(answers));
    }

    private Form form(FormStatus status) {
        Form form = new Form(mock(User.class), "폼", "설명", SLUG); // DRAFT
        ReflectionTestUtils.setField(form, "id", 10L);
        if (status != FormStatus.DRAFT) {
            form.changeStatus(FormStatus.PUBLISHED);
        }
        if (status == FormStatus.CLOSED) {
            form.changeStatus(FormStatus.CLOSED);
        }
        return form;
    }

    private Question question(Form form, Long id, QuestionType type, boolean required,
            Integer minValue, Integer maxValue) {
        Question question = new Question(form, type, "질문 " + id, required, 1, minValue, maxValue);
        ReflectionTestUtils.setField(question, "id", id);
        return question;
    }

    private QuestionOption option(Long id, Question question, String label) {
        QuestionOption option = new QuestionOption(question, label, 1);
        ReflectionTestUtils.setField(option, "id", id);
        return option;
    }
}
