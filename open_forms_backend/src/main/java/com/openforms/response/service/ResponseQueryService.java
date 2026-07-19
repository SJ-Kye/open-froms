package com.openforms.response.service;

import com.openforms.common.exception.ResourceNotFoundException;
import com.openforms.common.response.PageResponse;
import com.openforms.form.domain.Question;
import com.openforms.form.domain.QuestionOption;
import com.openforms.form.service.FormAccessGuard;
import com.openforms.form.service.QuestionLoader;
import com.openforms.response.domain.Answer;
import com.openforms.response.domain.Response;
import com.openforms.response.dto.AnswerDetail;
import com.openforms.response.dto.ResponseDetailResponse;
import com.openforms.response.dto.ResponseSummaryItem;
import com.openforms.response.repository.AnswerRepository;
import com.openforms.response.repository.ResponseRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 제작자의 응답 조회·삭제입니다. 소유권 검증은 {@link FormAccessGuard} 에 위임하여 폼·질문 API 와 같은
 * 규칙(존재 404 → 소유 403)을 그대로 따르고, 여기서는 <b>중첩 정합</b>(그 폼의 응답인가)과 저장 구조를
 * 화면이 읽는 모양으로 되묶는 일을 맡습니다.
 *
 * <p>저장은 "값 하나가 한 행"이라 체크박스 한 문항이 여러 {@link Answer} 행이 됩니다(04 문서). 화면은
 * 문항 단위로 읽으므로 상세 조립에서 질문 기준으로 다시 묶고, <b>답하지 않은 문항도 빈 값으로 포함</b>
 * 시켜 화면이 폼 구조를 따로 조회하지 않아도 되게 합니다.
 */
@Service
@Transactional(readOnly = true)
public class ResponseQueryService {

    private final FormAccessGuard formAccessGuard;
    private final QuestionLoader questionLoader;
    private final ResponseRepository responseRepository;
    private final AnswerRepository answerRepository;

    public ResponseQueryService(FormAccessGuard formAccessGuard, QuestionLoader questionLoader,
            ResponseRepository responseRepository, AnswerRepository answerRepository) {
        this.formAccessGuard = formAccessGuard;
        this.questionLoader = questionLoader;
        this.responseRepository = responseRepository;
        this.answerRepository = answerRepository;
    }

    /** 폼의 응답 목록입니다. 항목마다 "답한 문항 수 / 전체 문항 수"를 함께 담습니다. */
    public PageResponse<ResponseSummaryItem> list(Long formId, String email, Pageable pageable) {
        formAccessGuard.requireOwnedForm(formId, email);
        int totalQuestions = questionLoader.questionsOf(formId).size();
        Page<Response> page = responseRepository.findByForm_Id(formId, pageable);
        Map<Long, Long> answeredCounts = answeredCountsOf(page.getContent());

        return PageResponse.of(page.map(response -> new ResponseSummaryItem(
                response.getId(),
                response.getCreatedAt(),
                answeredCounts.getOrDefault(response.getId(), 0L),
                totalQuestions)));
    }

    /** 응답 1건의 상세입니다. 폼의 모든 문항을 순서대로 담되 무응답 문항은 빈 값으로 둡니다. */
    public ResponseDetailResponse get(Long formId, Long responseId, String email) {
        formAccessGuard.requireOwnedForm(formId, email);
        Response response = requireResponse(formId, responseId);

        Map<Long, List<Answer>> answersByQuestion = answerRepository.findByResponse_Id(responseId).stream()
                .collect(Collectors.groupingBy(answer -> answer.getQuestion().getId()));

        List<AnswerDetail> answers = questionLoader.questionsOf(formId).stream()
                .map(question -> toDetail(question, answersByQuestion.get(question.getId())))
                .toList();

        return new ResponseDetailResponse(response.getId(), response.getCreatedAt(), answers);
    }

    /**
     * 응답 1건을 삭제합니다. 답변 행을 <b>먼저 명시적으로</b> 지운 뒤 응답을 지웁니다.
     *
     * <p>{@code answers} 의 FK 에 ON DELETE CASCADE 가 걸려 있어 DB 만 보면 응답만 지워도 충분하지만,
     * 같은 트랜잭션에서 이미 상세를 조회했다면 {@link Answer} 들이 영속성 컨텍스트에 남아 삭제된 응답을
     * 참조한 채 flush 되어 실패합니다. DB 제약은 <b>데이터</b> 정합을, 이 호출은 <b>영속성 컨텍스트</b>
     * 정합을 담당하며 둘은 대체 관계가 아닙니다.
     */
    @Transactional
    public void delete(Long formId, Long responseId, String email) {
        formAccessGuard.requireOwnedForm(formId, email);
        Response response = requireResponse(formId, responseId);
        answerRepository.deleteByResponse_Id(responseId);
        responseRepository.delete(response);
    }

    /** 폼에 속한 응답을 가져옵니다. 다른 폼의 응답 id 는 없는 것과 같게 404 입니다(중첩 정합). */
    private Response requireResponse(Long formId, Long responseId) {
        return responseRepository.findByIdAndForm_Id(responseId, formId)
                .orElseThrow(() -> new ResourceNotFoundException("RESPONSE_NOT_FOUND",
                        "응답을 찾을 수 없습니다."));
    }

    /** 현재 페이지의 응답들이 각각 답한 문항 수입니다(응답마다 질의하지 않고 한 번에 집계). */
    private Map<Long, Long> answeredCountsOf(List<Response> responses) {
        if (responses.isEmpty()) {
            return Map.of();
        }
        List<Long> responseIds = responses.stream().map(Response::getId).toList();
        Map<Long, Long> counts = new HashMap<>();
        responseRepository.countAnsweredQuestionsByResponseIds(responseIds)
                .forEach(row -> counts.put(row.getResponseId(), row.getCnt()));
        return counts;
    }

    /** 한 문항의 답변입니다. 행이 없으면 무응답으로 표시합니다. */
    private AnswerDetail toDetail(Question question, List<Answer> answers) {
        if (answers == null || answers.isEmpty()) {
            return new AnswerDetail(question.getId(), question.getType(), question.getTitle(),
                    question.isRequired(), false, List.of(), null, null, null, null);
        }

        List<AnswerDetail.SelectedOption> selected = new ArrayList<>();
        for (Answer answer : answers) {
            QuestionOption option = answer.getOption();
            if (option != null) {
                selected.add(new AnswerDetail.SelectedOption(option.getId(), option.getLabel()));
            }
        }
        // 선택형이 아니면 값은 한 행에만 있으므로 첫 행에서 읽습니다.
        Answer first = answers.getFirst();
        return new AnswerDetail(question.getId(), question.getType(), question.getTitle(),
                question.isRequired(), true, List.copyOf(selected), first.getTextValue(),
                first.getNumberValue(), first.getDateValue(), first.getTimeValue());
    }
}
