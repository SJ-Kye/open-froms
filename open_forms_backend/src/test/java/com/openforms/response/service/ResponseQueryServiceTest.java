package com.openforms.response.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.openforms.common.exception.ResourceNotFoundException;
import com.openforms.common.response.PageResponse;
import com.openforms.form.domain.Form;
import com.openforms.form.domain.Question;
import com.openforms.form.domain.QuestionOption;
import com.openforms.form.domain.QuestionType;
import com.openforms.form.service.FormAccessGuard;
import com.openforms.form.service.QuestionLoader;
import com.openforms.response.domain.Answer;
import com.openforms.response.domain.Response;
import com.openforms.response.dto.AnswerDetail;
import com.openforms.response.dto.ResponseDetailResponse;
import com.openforms.response.dto.ResponseSummaryItem;
import com.openforms.response.repository.AnswerRepository;
import com.openforms.response.repository.ResponseRepository;
import com.openforms.user.domain.User;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 응답 조회·삭제 규칙을 먼저 고정합니다. 소유권은 {@link FormAccessGuard} 에 위임하므로 여기서 검증할
 * 것은 <b>중첩 정합</b>(다른 폼의 응답 id 를 넣으면 404)과 <b>상세 조립</b>(값 하나가 한 행인 저장 구조를
 * 문항 단위로 되묶고, 무응답 문항도 빠뜨리지 않는 것)입니다.
 */
@ExtendWith(MockitoExtension.class)
class ResponseQueryServiceTest {

    private static final Long FORM_ID = 10L;
    private static final Long RESPONSE_ID = 500L;
    private static final String EMAIL = "owner@example.com";

    @Mock
    private FormAccessGuard formAccessGuard;
    @Mock
    private QuestionLoader questionLoader;
    @Mock
    private ResponseRepository responseRepository;
    @Mock
    private AnswerRepository answerRepository;
    @InjectMocks
    private ResponseQueryService service;

    @Test
    @DisplayName("소유자가 아니면 응답을 볼 수 없다 → 403")
    void rejectsNonOwner() {
        when(formAccessGuard.requireOwnedForm(FORM_ID, EMAIL))
                .thenThrow(new AccessDeniedException("접근 권한이 없습니다."));

        assertThatThrownBy(() -> service.list(FORM_ID, EMAIL, PageRequest.of(0, 20)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("다른 폼의 응답 id 로 조회하면 404(RESPONSE_NOT_FOUND)")
    void rejectsResponseOfAnotherForm() {
        Form form = form();
        when(formAccessGuard.requireOwnedForm(FORM_ID, EMAIL)).thenReturn(form);
        when(responseRepository.findByIdAndForm_Id(RESPONSE_ID, FORM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(FORM_ID, RESPONSE_ID, EMAIL))
                .asInstanceOf(InstanceOfAssertFactories.type(ResourceNotFoundException.class))
                .satisfies(ex -> assertThat(ex.getCode()).isEqualTo("RESPONSE_NOT_FOUND"));
    }

    @Test
    @DisplayName("다른 폼의 응답은 삭제되지 않는다 → 404, 삭제 호출도 없음")
    void doesNotDeleteResponseOfAnotherForm() {
        when(formAccessGuard.requireOwnedForm(FORM_ID, EMAIL)).thenReturn(form());
        when(responseRepository.findByIdAndForm_Id(RESPONSE_ID, FORM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(FORM_ID, RESPONSE_ID, EMAIL))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(responseRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("목록은 응답마다 답한 문항 수와 전체 문항 수를 함께 담는다")
    void listCarriesAnsweredCounts() {
        Form form = form();
        Response response = response(RESPONSE_ID, form);
        when(formAccessGuard.requireOwnedForm(FORM_ID, EMAIL)).thenReturn(form);
        when(questionLoader.questionsOf(FORM_ID)).thenReturn(List.of(
                question(form, 1L, QuestionType.SHORT_TEXT, true),
                question(form, 2L, QuestionType.RATING, false),
                question(form, 3L, QuestionType.DATE, false)));
        Pageable pageable = PageRequest.of(0, 20);
        when(responseRepository.findByForm_Id(FORM_ID, pageable))
                .thenReturn(new PageImpl<>(List.of(response), pageable, 1));
        when(responseRepository.countAnsweredQuestionsByResponseIds(List.of(RESPONSE_ID)))
                .thenReturn(List.of(answeredCount(RESPONSE_ID, 2L)));

        PageResponse<ResponseSummaryItem> page = service.list(FORM_ID, EMAIL, pageable);

        assertThat(page.content()).singleElement().satisfies(item -> {
            assertThat(item.responseId()).isEqualTo(RESPONSE_ID);
            assertThat(item.answeredCount()).isEqualTo(2);
            assertThat(item.totalQuestions()).isEqualTo(3);
        });
    }

    @Test
    @DisplayName("답변 행이 하나도 없는 응답은 답한 문항 수가 0 이다")
    void listShowsZeroForResponseWithoutAnswers() {
        Form form = form();
        Pageable pageable = PageRequest.of(0, 20);
        when(formAccessGuard.requireOwnedForm(FORM_ID, EMAIL)).thenReturn(form);
        when(questionLoader.questionsOf(FORM_ID))
                .thenReturn(List.of(question(form, 1L, QuestionType.SHORT_TEXT, false)));
        when(responseRepository.findByForm_Id(FORM_ID, pageable))
                .thenReturn(new PageImpl<>(List.of(response(RESPONSE_ID, form)), pageable, 1));
        when(responseRepository.countAnsweredQuestionsByResponseIds(List.of(RESPONSE_ID)))
                .thenReturn(List.of());

        PageResponse<ResponseSummaryItem> page = service.list(FORM_ID, EMAIL, pageable);

        assertThat(page.content().getFirst().answeredCount()).isZero();
    }

    @Test
    @DisplayName("상세는 체크박스의 여러 행을 한 문항으로 묶고, 무응답 문항도 포함한다")
    void detailGroupsRowsByQuestionAndKeepsUnanswered() {
        Form form = form();
        Response response = response(RESPONSE_ID, form);
        Question checkbox = question(form, 1L, QuestionType.MULTIPLE_CHOICE, true);
        Question unanswered = question(form, 2L, QuestionType.SHORT_TEXT, false);
        QuestionOption first = option(100L, checkbox, "가");
        QuestionOption second = option(101L, checkbox, "나");
        when(formAccessGuard.requireOwnedForm(FORM_ID, EMAIL)).thenReturn(form);
        when(responseRepository.findByIdAndForm_Id(RESPONSE_ID, FORM_ID)).thenReturn(Optional.of(response));
        when(questionLoader.questionsOf(FORM_ID)).thenReturn(List.of(checkbox, unanswered));
        when(answerRepository.findByResponse_Id(RESPONSE_ID)).thenReturn(List.of(
                Answer.ofOption(response, checkbox, first),
                Answer.ofOption(response, checkbox, second)));

        ResponseDetailResponse detail = service.get(FORM_ID, RESPONSE_ID, EMAIL);

        assertThat(detail.answers()).hasSize(2);
        AnswerDetail answered = detail.answers().getFirst();
        assertThat(answered.answered()).isTrue();
        assertThat(answered.selectedOptions())
                .extracting(AnswerDetail.SelectedOption::label)
                .containsExactly("가", "나");
        AnswerDetail skipped = detail.answers().get(1);
        assertThat(skipped.answered()).isFalse();
        assertThat(skipped.selectedOptions()).isEmpty();
        assertThat(skipped.textValue()).isNull();
    }

    // --- fixtures ---

    private Form form() {
        User user = new User(EMAIL, "hash", "제작자");
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        Form form = new Form(user, "설문", "설명", "slug1234");
        ReflectionTestUtils.setField(form, "id", FORM_ID);
        return form;
    }

    private Response response(Long id, Form form) {
        Response response = new Response(form);
        ReflectionTestUtils.setField(response, "id", id);
        return response;
    }

    private Question question(Form form, Long id, QuestionType type, boolean required) {
        Question question = new Question(form, type, "질문 " + id, required, id.intValue(), null, null);
        ReflectionTestUtils.setField(question, "id", id);
        return question;
    }

    private QuestionOption option(Long id, Question question, String label) {
        QuestionOption option = new QuestionOption(question, label, id.intValue());
        ReflectionTestUtils.setField(option, "id", id);
        return option;
    }

    private ResponseRepository.AnsweredCountRow answeredCount(Long responseId, long count) {
        ResponseRepository.AnsweredCountRow row =
                org.mockito.Mockito.mock(ResponseRepository.AnsweredCountRow.class);
        when(row.getResponseId()).thenReturn(responseId);
        when(row.getCnt()).thenReturn(count);
        return row;
    }
}
