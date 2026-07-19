package com.openforms.response.service;

import com.openforms.form.domain.Form;
import com.openforms.form.domain.Question;
import com.openforms.form.domain.QuestionOption;
import com.openforms.form.domain.QuestionType;
import com.openforms.form.service.FormAccessGuard;
import com.openforms.form.service.QuestionLoader;
import com.openforms.response.dto.FormSummaryStats;
import com.openforms.response.dto.QuestionStats;
import com.openforms.response.repository.AnswerRepository;
import com.openforms.response.repository.ResponseRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 대시보드 집계입니다. 계산은 애플리케이션 메모리가 아니라 <b>DB 집계 질의</b>로 수행합니다 — 응답이
 * 늘수록 전 행을 읽어 세는 방식은 비용이 선형으로 커지고, 04 에서 집계 경로에 맞춰 심어 둔 인덱스
 * ({@code ix_responses_form_created}·{@code ix_answers_question_option})가 쓰이지 않습니다.
 *
 * <p>DB 가 답하기 어려운 두 가지만 여기서 처리합니다: <b>응답이 없던 날을 0 으로 채우는 일</b>(DB 에
 * 날짜 생성을 요구하면 PostgreSQL 전용 문법이 됩니다)과 <b>아무도 고르지 않은 선택지를 0 으로 채우는
 * 일</b>(집계 결과에는 그 선택지의 행이 아예 없습니다). 둘 다 "없는 것"을 채우는 일이라 질의 결과만으로는
 * 알 수 없고, 폼 구조를 아는 쪽이 메워야 합니다.
 */
@Service
@Transactional(readOnly = true)
public class FormSummaryService {

    /** 텍스트형 문항에서 함께 내려보낼 최근 응답 수입니다. 전문은 응답 상세에서 확인합니다. */
    private static final int RECENT_TEXT_SAMPLES = 5;

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
        Form form = formAccessGuard.requireOwnedForm(formId, email);
        List<Question> questions = questionLoader.questionsOf(formId);
        long totalResponses = responseRepository.countByForm_Id(formId);

        return new FormSummaryStats(
                formId,
                totalResponses,
                completionRate(formId, totalResponses, questions.size()),
                dailyCounts(form, formId),
                questionStats(formId, questions));
    }

    /**
     * 완료율은 응답 1건당 "답한 질문 수 / 전체 질문 수" 의 평균입니다. 응답마다 비율을 구해 평균 내는
     * 대신, 답한 (응답, 질문) 쌍의 총수를 {@code 총 응답 수 × 전체 질문 수} 로 나눕니다 — 값은 같고
     * 질의는 한 번이며, 답변 행이 하나도 없는 응답도 분모에 남아 0 으로 반영됩니다.
     */
    private double completionRate(Long formId, long totalResponses, int totalQuestions) {
        if (totalResponses == 0 || totalQuestions == 0) {
            return 0.0;
        }
        long answeredPairs = responseRepository.countAnsweredQuestionPairs(formId);
        return (double) answeredPairs / (totalResponses * (long) totalQuestions);
    }

    /**
     * 발행일부터 종료일(종료 전이면 오늘)까지 하루도 빠뜨리지 않은 응답 추이입니다. 발행 전 폼은 응답을
     * 받을 수 있던 날이 하루도 없으므로 빈 목록입니다.
     */
    private List<FormSummaryStats.DailyCount> dailyCounts(Form form, Long formId) {
        LocalDateTime publishedAt = form.getPublishedAt();
        if (publishedAt == null) {
            return List.of();
        }
        LocalDate from = publishedAt.toLocalDate();
        LocalDateTime closedAt = form.getClosedAt();
        LocalDate to = closedAt != null ? closedAt.toLocalDate() : LocalDate.now();
        if (to.isBefore(from)) {
            to = from;
        }

        Map<LocalDate, Long> counted = new HashMap<>();
        responseRepository.countByDate(formId)
                .forEach(row -> counted.put(row.getResponseDay(), row.getCnt()));

        List<FormSummaryStats.DailyCount> daily = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            daily.add(new FormSummaryStats.DailyCount(date, counted.getOrDefault(date, 0L)));
        }
        return List.copyOf(daily);
    }

    /** 문항별 분포입니다. 질문 순서(position)를 그대로 유지해 화면이 폼과 같은 순서로 그릴 수 있습니다. */
    private List<QuestionStats> questionStats(Long formId, List<Question> questions) {
        if (questions.isEmpty()) {
            return List.of();
        }
        Map<Long, List<QuestionOption>> optionsByQuestion = questionLoader.optionsByQuestionId(questions);
        Map<Long, Long> respondents = new HashMap<>();
        answerRepository.countRespondentsByQuestion(formId)
                .forEach(row -> respondents.put(row.getQuestionId(), row.getCnt()));
        Map<Long, Map<Long, Long>> optionCounts = optionCounts(formId);
        Map<Long, List<QuestionStats.ValueCount>> valueCounts = valueCounts(formId);

        List<QuestionStats> stats = new ArrayList<>();
        for (Question question : questions) {
            Long questionId = question.getId();
            QuestionType type = question.getType();
            List<QuestionStats.ValueCount> values =
                    valueCounts.getOrDefault(questionId, List.of());
            stats.add(new QuestionStats(
                    questionId, type, question.getTitle(), question.isRequired(),
                    respondents.getOrDefault(questionId, 0L),
                    type.hasRange() ? average(values) : null,
                    type.isChoice()
                            ? optionCountsOf(optionsByQuestion.get(questionId),
                                    optionCounts.getOrDefault(questionId, Map.of()))
                            : List.of(),
                    type.hasRange() ? values : List.of(),
                    isText(type) ? recentTexts(questionId) : List.of()));
        }
        return List.copyOf(stats);
    }

    /** 질문 → (선택지 → 선택 수) 로 접은 집계 결과입니다. */
    private Map<Long, Map<Long, Long>> optionCounts(Long formId) {
        Map<Long, Map<Long, Long>> byQuestion = new HashMap<>();
        answerRepository.countByOption(formId).forEach(row -> byQuestion
                .computeIfAbsent(row.getQuestionId(), key -> new HashMap<>())
                .put(row.getOptionId(), row.getCnt()));
        return byQuestion;
    }

    /** 질문 → 값별 개수(값 오름차순)로 접은 집계 결과입니다. */
    private Map<Long, List<QuestionStats.ValueCount>> valueCounts(Long formId) {
        Map<Long, List<QuestionStats.ValueCount>> byQuestion = new LinkedHashMap<>();
        answerRepository.countByNumberValue(formId).forEach(row -> byQuestion
                .computeIfAbsent(row.getQuestionId(), key -> new ArrayList<>())
                .add(new QuestionStats.ValueCount(row.getAnswerValue(), row.getCnt())));
        return byQuestion;
    }

    /**
     * 선택지별 선택 수입니다. 폼의 선택지를 기준으로 순회하므로 <b>아무도 고르지 않은 선택지도 0 으로</b>
     * 남습니다 — 집계 결과만 쓰면 그 선택지가 차트에서 통째로 사라져 "선택지가 없었던 것"처럼 보입니다.
     */
    private List<QuestionStats.OptionCount> optionCountsOf(List<QuestionOption> options,
            Map<Long, Long> counts) {
        if (options == null || options.isEmpty()) {
            return List.of();
        }
        return options.stream()
                .map(option -> new QuestionStats.OptionCount(option.getId(), option.getLabel(),
                        counts.getOrDefault(option.getId(), 0L)))
                .toList();
    }

    /** 값별 개수로부터 계산한 평균입니다(값×개수의 합 ÷ 개수의 합). 응답이 없으면 null 입니다. */
    private Double average(List<QuestionStats.ValueCount> values) {
        long count = values.stream().mapToLong(QuestionStats.ValueCount::count).sum();
        if (count == 0) {
            return null;
        }
        long sum = values.stream()
                .mapToLong(value -> (long) value.value() * value.count())
                .sum();
        return (double) sum / count;
    }

    private List<String> recentTexts(Long questionId) {
        return answerRepository.findRecentTexts(questionId, PageRequest.of(0, RECENT_TEXT_SAMPLES));
    }

    private boolean isText(QuestionType type) {
        return type == QuestionType.SHORT_TEXT || type == QuestionType.LONG_TEXT;
    }
}
