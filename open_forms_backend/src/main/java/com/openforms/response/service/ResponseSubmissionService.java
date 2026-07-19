package com.openforms.response.service;

import com.openforms.common.exception.BadRequestException;
import com.openforms.common.exception.ConflictException;
import com.openforms.common.exception.ErrorResponse;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 익명 응답 제출 규칙의 단일 지점입니다. slug 해석(미발행 404)은 {@link PublicFormService} 에 위임하고,
 * 여기서는 제출 가능 여부(종료 409)와 질문 타입별 값 검증(400)을 담당합니다.
 *
 * <p>검증은 <b>404 → 409 → 400</b> 순서입니다. 폼에 닿을 수 있는지, 지금 받을 수 있는 상태인지를 먼저
 * 가린 뒤에야 본문을 따집니다. 저장은 05·04 문서대로 "값 하나가 한 행"이라, 선택형은 고른 선택지
 * 개수만큼 {@link Answer} 행이 생기고 그 외 타입은 1행입니다.
 */
@Service
@Transactional(readOnly = true)
public class ResponseSubmissionService {

    private final PublicFormService publicFormService;
    private final QuestionLoader questionLoader;
    private final ResponseRepository responseRepository;
    private final AnswerRepository answerRepository;

    public ResponseSubmissionService(PublicFormService publicFormService, QuestionLoader questionLoader,
            ResponseRepository responseRepository, AnswerRepository answerRepository) {
        this.publicFormService = publicFormService;
        this.questionLoader = questionLoader;
        this.responseRepository = responseRepository;
        this.answerRepository = answerRepository;
    }

    @Transactional
    public SubmitResponseResult submit(String slug, SubmitResponseRequest request) {
        Form form = publicFormService.requireVisibleForm(slug); // 없음·미발행 → 404
        if (form.getStatus() == FormStatus.CLOSED) {
            throw new ConflictException("FORM_CLOSED", "종료된 설문지에는 응답할 수 없습니다.");
        }

        List<Question> questions = questionLoader.questionsOf(form.getId());
        Map<Long, List<QuestionOption>> optionsByQuestion = questionLoader.optionsByQuestionId(questions);
        Map<Long, AnswerRequest> submitted = indexByQuestion(request.answers(), questions);
        requireAllRequiredAnswered(questions, submitted);

        Response response = responseRepository.save(new Response(form));
        List<Answer> rows = new ArrayList<>();
        for (Question question : questions) {
            AnswerRequest answer = submitted.get(question.getId());
            if (answer != null) {
                rows.addAll(toAnswers(response, question, answer,
                        optionsByQuestion.getOrDefault(question.getId(), List.of())));
            }
        }
        answerRepository.saveAll(rows);
        return SubmitResponseResult.from(response);
    }

    /** 제출 항목을 질문 id 로 색인합니다. 폼 밖 질문과 중복 제출을 여기서 걸러냅니다. */
    private Map<Long, AnswerRequest> indexByQuestion(List<AnswerRequest> answers, List<Question> questions) {
        List<Long> questionIds = questions.stream().map(Question::getId).toList();
        Map<Long, AnswerRequest> byQuestion = new LinkedHashMap<>();
        for (AnswerRequest answer : answers) {
            if (!questionIds.contains(answer.questionId())) {
                throw new BadRequestException("UNKNOWN_QUESTION",
                        "이 설문지에 없는 질문입니다: " + answer.questionId());
            }
            if (byQuestion.putIfAbsent(answer.questionId(), answer) != null) {
                throw new BadRequestException("DUPLICATE_ANSWER",
                        "같은 질문에 두 번 응답했습니다: " + answer.questionId());
            }
        }
        return byQuestion;
    }

    /** 필수 질문 중 응답이 없거나 값이 비어 있는 것을 모아 한 번에 반송합니다(누락 질문별 fieldErrors). */
    private void requireAllRequiredAnswered(List<Question> questions, Map<Long, AnswerRequest> submitted) {
        List<ErrorResponse.FieldError> missing = questions.stream()
                .filter(Question::isRequired)
                .filter(question -> isBlank(question, submitted.get(question.getId())))
                .map(question -> new ErrorResponse.FieldError("questionId:" + question.getId(),
                        "필수 응답입니다."))
                .toList();
        if (!missing.isEmpty()) {
            throw new BadRequestException("REQUIRED_ANSWER_MISSING", "필수 질문에 응답하지 않았습니다.", missing);
        }
    }

    /** 항목이 아예 없거나, 타입에 해당하는 값이 비어 있으면 미응답으로 봅니다. */
    private boolean isBlank(Question question, AnswerRequest answer) {
        if (answer == null) {
            return true;
        }
        return switch (question.getType()) {
            case SHORT_TEXT, LONG_TEXT -> answer.textValue() == null || answer.textValue().isBlank();
            case SINGLE_CHOICE, DROPDOWN, MULTIPLE_CHOICE ->
                    answer.selectedOptionIds() == null || answer.selectedOptionIds().isEmpty();
            case RATING, NUMBER -> answer.numberValue() == null;
            case DATE -> answer.dateValue() == null;
            case TIME -> answer.timeValue() == null;
        };
    }

    /** 질문 타입에 맞는 값을 검증해 저장할 행으로 만듭니다. 선택형만 여러 행이 될 수 있습니다. */
    private List<Answer> toAnswers(Response response, Question question, AnswerRequest answer,
            List<QuestionOption> options) {
        return switch (question.getType()) {
            case SHORT_TEXT, LONG_TEXT -> {
                requireValue(question, answer.textValue() != null && !answer.textValue().isBlank());
                yield List.of(Answer.ofText(response, question, answer.textValue()));
            }
            case SINGLE_CHOICE, DROPDOWN, MULTIPLE_CHOICE ->
                    selectedOptions(question, answer, options).stream()
                            .map(option -> Answer.ofOption(response, question, option))
                            .toList();
            case RATING, NUMBER -> {
                requireValue(question, answer.numberValue() != null);
                requireInRange(question, answer.numberValue());
                yield List.of(Answer.ofNumber(response, question, answer.numberValue()));
            }
            case DATE -> {
                requireValue(question, answer.dateValue() != null);
                yield List.of(Answer.ofDate(response, question, answer.dateValue()));
            }
            case TIME -> {
                requireValue(question, answer.timeValue() != null);
                yield List.of(Answer.ofTime(response, question, answer.timeValue()));
            }
        };
    }

    /** 고른 선택지가 그 질문의 것인지, 택1 질문에 하나만 골랐는지 확인합니다. */
    private List<QuestionOption> selectedOptions(Question question, AnswerRequest answer,
            List<QuestionOption> options) {
        List<Long> selectedIds = answer.selectedOptionIds();
        requireValue(question, selectedIds != null && !selectedIds.isEmpty());
        if (question.getType() != QuestionType.MULTIPLE_CHOICE && selectedIds.size() > 1) {
            throw invalidValue(question, "선택지를 하나만 고를 수 있습니다.");
        }
        return selectedIds.stream()
                .map(id -> options.stream().filter(option -> option.getId().equals(id)).findFirst()
                        .orElseThrow(() -> invalidValue(question, "이 질문의 선택지가 아닙니다: " + id)))
                .toList();
    }

    private void requireValue(Question question, boolean satisfied) {
        if (!satisfied) {
            throw invalidValue(question, question.getType() + " 질문에 맞는 값이 없습니다.");
        }
    }

    /** 질문이 허용 범위를 정의한 경우에만 확인합니다(3B 에서 min/max 는 선택 항목). */
    private void requireInRange(Question question, Integer value) {
        boolean belowMin = question.getMinValue() != null && value < question.getMinValue();
        boolean aboveMax = question.getMaxValue() != null && value > question.getMaxValue();
        if (belowMin || aboveMax) {
            throw new BadRequestException("ANSWER_OUT_OF_RANGE",
                    "허용 범위를 벗어난 값입니다: questionId=" + question.getId() + ", value=" + value);
        }
    }

    private BadRequestException invalidValue(Question question, String reason) {
        return new BadRequestException("INVALID_ANSWER_VALUE",
                "questionId=" + question.getId() + ": " + reason);
    }
}
