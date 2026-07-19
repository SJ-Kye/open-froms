package com.openforms.response.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.openforms.form.domain.Form;
import com.openforms.form.domain.FormStatus;
import com.openforms.form.domain.Question;
import com.openforms.form.domain.QuestionOption;
import com.openforms.form.domain.QuestionType;
import com.openforms.form.service.FormAccessGuard;
import com.openforms.form.service.QuestionLoader;
import com.openforms.response.dto.FormSummaryStats;
import com.openforms.response.dto.QuestionStats;
import com.openforms.response.repository.AnswerRepository;
import com.openforms.response.repository.ResponseRepository;
import com.openforms.user.domain.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 대시보드 집계 규칙을 먼저 고정합니다. 핵심은 <b>완료율의 정의</b>(응답 1건당 답한 질문 수 / 전체 질문
 * 수의 평균)와 <b>일별 추이의 구간</b>(발행일~종료일, 빈 날은 0)입니다. 둘 다 "값이 그럴듯하게 나오는지"가
 * 아니라 정의대로 나오는지를 숫자로 못박아야 이후 구현이 흔들리지 않습니다.
 */
@ExtendWith(MockitoExtension.class)
class FormSummaryServiceTest {

    private static final Long FORM_ID = 10L;
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
    private FormSummaryService service;

    @Test
    @DisplayName("소유자가 아니면 집계를 볼 수 없다 → 403")
    void rejectsNonOwner() {
        when(formAccessGuard.requireOwnedForm(FORM_ID, EMAIL))
                .thenThrow(new AccessDeniedException("접근 권한이 없습니다."));

        assertThatThrownBy(() -> service.summarize(FORM_ID, EMAIL))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("완료율은 응답별 응답률의 평균이다 — 질문 5개·응답 2건·답변 쌍 6개 → 0.6")
    void completionRateIsAverageAnswerRate() {
        Form form = publishedForm(LocalDateTime.now().minusDays(1), null);
        givenForm(form, 5);
        when(responseRepository.countByForm_Id(FORM_ID)).thenReturn(2L);
        when(responseRepository.countAnsweredQuestionPairs(FORM_ID)).thenReturn(6L);

        FormSummaryStats stats = service.summarize(FORM_ID, EMAIL);

        assertThat(stats.totalResponses()).isEqualTo(2);
        assertThat(stats.completionRate()).isCloseTo(0.6, within(0.0001));
    }

    @Test
    @DisplayName("응답이 없거나 질문이 없으면 완료율은 0.0 이다 — 0 으로 나누지 않는다")
    void completionRateIsZeroWithoutResponses() {
        Form form = publishedForm(LocalDateTime.now(), null);
        givenForm(form, 0);
        when(responseRepository.countByForm_Id(FORM_ID)).thenReturn(0L);

        FormSummaryStats stats = service.summarize(FORM_ID, EMAIL);

        assertThat(stats.completionRate()).isEqualTo(0.0);
        assertThat(stats.questionSummaries()).isEmpty();
    }

    @Test
    @DisplayName("일별 추이는 발행일부터 종료일까지 빈 날을 0 으로 채워 이어진다")
    void dailyCountsFillGapsBetweenPublishAndClose() {
        LocalDate publishedOn = LocalDate.now().minusDays(4);
        Form form = publishedForm(publishedOn.atTime(9, 0), publishedOn.plusDays(3).atTime(18, 0));
        givenForm(form, 1);
        when(responseRepository.countByForm_Id(FORM_ID)).thenReturn(3L);
        when(responseRepository.countAnsweredQuestionPairs(FORM_ID)).thenReturn(3L);
        when(responseRepository.countByDate(FORM_ID)).thenReturn(List.of(
                dailyRow(publishedOn, 1L),
                dailyRow(publishedOn.plusDays(2), 2L)));

        FormSummaryStats stats = service.summarize(FORM_ID, EMAIL);

        assertThat(stats.responsesByDate()).extracting(FormSummaryStats.DailyCount::date)
                .containsExactly(publishedOn, publishedOn.plusDays(1), publishedOn.plusDays(2),
                        publishedOn.plusDays(3));
        assertThat(stats.responsesByDate()).extracting(FormSummaryStats.DailyCount::count)
                .containsExactly(1L, 0L, 2L, 0L);
    }

    @Test
    @DisplayName("발행 전 폼은 추이가 빈 목록이다 — 응답을 받을 수 있던 날이 하루도 없다")
    void draftFormHasNoDateRange() {
        Form form = new Form(owner(), "설문", "설명", "slug1234");
        givenForm(form, 2);
        when(responseRepository.countByForm_Id(FORM_ID)).thenReturn(0L);

        FormSummaryStats stats = service.summarize(FORM_ID, EMAIL);

        assertThat(stats.responsesByDate()).isEmpty();
    }

    @Test
    @DisplayName("선택형은 아무도 고르지 않은 선택지도 0 으로 포함한다")
    void optionCountsIncludeUnchosenOptions() {
        Form form = publishedForm(LocalDateTime.now(), null);
        Question choice = question(form, 1L, QuestionType.SINGLE_CHOICE);
        QuestionOption picked = option(100L, choice, "만족");
        QuestionOption ignored = option(101L, choice, "불만족");
        when(formAccessGuard.requireOwnedForm(FORM_ID, EMAIL)).thenReturn(form);
        when(questionLoader.questionsOf(FORM_ID)).thenReturn(List.of(choice));
        when(questionLoader.optionsByQuestionId(List.of(choice)))
                .thenReturn(Map.of(1L, List.of(picked, ignored)));
        when(responseRepository.countByForm_Id(FORM_ID)).thenReturn(4L);
        when(responseRepository.countAnsweredQuestionPairs(FORM_ID)).thenReturn(4L);
        when(answerRepository.countRespondentsByQuestion(FORM_ID)).thenReturn(List.of(questionCount(1L, 4L)));
        when(answerRepository.countByOption(FORM_ID)).thenReturn(List.of(optionCount(1L, 100L, 4L)));

        FormSummaryStats stats = service.summarize(FORM_ID, EMAIL);

        QuestionStats question = stats.questionSummaries().getFirst();
        assertThat(question.answeredCount()).isEqualTo(4);
        assertThat(question.optionCounts())
                .extracting(QuestionStats.OptionCount::label, QuestionStats.OptionCount::count)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("만족", 4L),
                        org.assertj.core.groups.Tuple.tuple("불만족", 0L));
        assertThat(question.valueCounts()).isEmpty();
        assertThat(question.recentTexts()).isEmpty();
    }

    @Test
    @DisplayName("평점형의 평균은 값별 개수로부터 계산한다 — (1×1 + 5×3) / 4 = 4.0")
    void ratingAverageIsWeightedByValueCounts() {
        Form form = publishedForm(LocalDateTime.now(), null);
        Question rating = question(form, 2L, QuestionType.RATING);
        when(formAccessGuard.requireOwnedForm(FORM_ID, EMAIL)).thenReturn(form);
        when(questionLoader.questionsOf(FORM_ID)).thenReturn(List.of(rating));
        when(questionLoader.optionsByQuestionId(List.of(rating))).thenReturn(Map.of());
        when(responseRepository.countByForm_Id(FORM_ID)).thenReturn(4L);
        when(responseRepository.countAnsweredQuestionPairs(FORM_ID)).thenReturn(4L);
        when(answerRepository.countRespondentsByQuestion(FORM_ID)).thenReturn(List.of(questionCount(2L, 4L)));
        when(answerRepository.countByNumberValue(FORM_ID))
                .thenReturn(List.of(valueCount(2L, 1, 1L), valueCount(2L, 5, 3L)));

        FormSummaryStats stats = service.summarize(FORM_ID, EMAIL);

        QuestionStats question = stats.questionSummaries().getFirst();
        assertThat(question.average()).isCloseTo(4.0, within(0.0001));
        assertThat(question.valueCounts())
                .extracting(QuestionStats.ValueCount::value, QuestionStats.ValueCount::count)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(1, 1L),
                        org.assertj.core.groups.Tuple.tuple(5, 3L));
        assertThat(question.optionCounts()).isEmpty();
    }

    @Test
    @DisplayName("텍스트형은 평균 없이 최근 응답 일부만 담는다")
    void textQuestionCarriesRecentSamplesOnly() {
        Form form = publishedForm(LocalDateTime.now(), null);
        Question text = question(form, 3L, QuestionType.LONG_TEXT);
        when(formAccessGuard.requireOwnedForm(FORM_ID, EMAIL)).thenReturn(form);
        when(questionLoader.questionsOf(FORM_ID)).thenReturn(List.of(text));
        when(questionLoader.optionsByQuestionId(List.of(text))).thenReturn(Map.of());
        when(responseRepository.countByForm_Id(FORM_ID)).thenReturn(2L);
        when(responseRepository.countAnsweredQuestionPairs(FORM_ID)).thenReturn(2L);
        when(answerRepository.countRespondentsByQuestion(FORM_ID)).thenReturn(List.of(questionCount(3L, 2L)));
        when(answerRepository.findRecentTexts(eq(3L), any()))
                .thenReturn(List.of("친절했습니다", "배송이 빨랐어요"));

        FormSummaryStats stats = service.summarize(FORM_ID, EMAIL);

        QuestionStats question = stats.questionSummaries().getFirst();
        assertThat(question.recentTexts()).containsExactly("친절했습니다", "배송이 빨랐어요");
        assertThat(question.average()).isNull();
        assertThat(question.optionCounts()).isEmpty();
    }

    // --- fixtures ---

    private void givenForm(Form form, int questionCount) {
        List<Question> questions = new java.util.ArrayList<>();
        for (long i = 1; i <= questionCount; i++) {
            questions.add(question(form, i, QuestionType.SHORT_TEXT));
        }
        when(formAccessGuard.requireOwnedForm(FORM_ID, EMAIL)).thenReturn(form);
        when(questionLoader.questionsOf(FORM_ID)).thenReturn(questions);
        lenient().when(questionLoader.optionsByQuestionId(questions)).thenReturn(Map.of());
        lenient().when(answerRepository.countRespondentsByQuestion(FORM_ID)).thenReturn(List.of());
        lenient().when(answerRepository.findRecentTexts(anyLong(), any())).thenReturn(List.of());
    }

    private User owner() {
        User user = new User(EMAIL, "hash", "제작자");
        ReflectionTestUtils.setField(user, "id", java.util.UUID.randomUUID());
        return user;
    }

    private Form publishedForm(LocalDateTime publishedAt, LocalDateTime closedAt) {
        Form form = new Form(owner(), "설문", "설명", "slug1234");
        ReflectionTestUtils.setField(form, "id", FORM_ID);
        ReflectionTestUtils.setField(form, "status",
                closedAt == null ? FormStatus.PUBLISHED : FormStatus.CLOSED);
        ReflectionTestUtils.setField(form, "publishedAt", publishedAt);
        ReflectionTestUtils.setField(form, "closedAt", closedAt);
        return form;
    }

    private Question question(Form form, Long id, QuestionType type) {
        Question question = new Question(form, type, "질문 " + id, true, id.intValue(), null, null);
        ReflectionTestUtils.setField(question, "id", id);
        return question;
    }

    private QuestionOption option(Long id, Question question, String label) {
        QuestionOption option = new QuestionOption(question, label, 1);
        ReflectionTestUtils.setField(option, "id", id);
        return option;
    }

    private ResponseRepository.DailyCountRow dailyRow(LocalDate date, long count) {
        ResponseRepository.DailyCountRow row = mock(ResponseRepository.DailyCountRow.class);
        when(row.getDay()).thenReturn(java.sql.Date.valueOf(date));
        when(row.getCnt()).thenReturn(count);
        return row;
    }

    private AnswerRepository.QuestionCountRow questionCount(Long questionId, long count) {
        AnswerRepository.QuestionCountRow row = mock(AnswerRepository.QuestionCountRow.class);
        when(row.getQuestionId()).thenReturn(questionId);
        when(row.getCnt()).thenReturn(count);
        return row;
    }

    private AnswerRepository.OptionCountRow optionCount(Long questionId, Long optionId, long count) {
        AnswerRepository.OptionCountRow row = mock(AnswerRepository.OptionCountRow.class);
        lenient().when(row.getQuestionId()).thenReturn(questionId);
        when(row.getOptionId()).thenReturn(optionId);
        when(row.getCnt()).thenReturn(count);
        return row;
    }

    private AnswerRepository.ValueCountRow valueCount(Long questionId, int value, long count) {
        AnswerRepository.ValueCountRow row = mock(AnswerRepository.ValueCountRow.class);
        lenient().when(row.getQuestionId()).thenReturn(questionId);
        when(row.getValue()).thenReturn(value);
        when(row.getCnt()).thenReturn(count);
        return row;
    }
}
