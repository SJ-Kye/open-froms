package com.openforms.response.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.openforms.form.domain.QuestionType;
import com.openforms.form.dto.FormDetailResponse;
import com.openforms.form.dto.OptionRequest;
import com.openforms.form.dto.QuestionRequest;
import com.openforms.form.dto.QuestionResponse;
import com.openforms.response.dto.AnswerRequest;
import com.openforms.response.dto.SubmitResponseRequest;
import com.openforms.response.dto.SubmitResponseResult;
import com.openforms.user.dto.LoginRequest;
import com.openforms.user.dto.RegisterRequest;
import com.openforms.user.dto.TokenResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * 응답 조회·삭제와 집계를 실제 보안 체인·실제 집계 질의와 함께 검증합니다. 단위 테스트가 모의 객체로
 * 고정한 규칙(완료율 정의·선택지 0 채우기)이 <b>진짜 SQL 로도 같은 값</b>을 내는지 확인하는 것이 목적이며,
 * 특히 체크박스 저장이 여러 행이라는 점 때문에 DISTINCT 를 빠뜨리면 여기서 드러납니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ResponseControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("응답 목록 → 200, 항목마다 답한 문항 수/전체 문항 수")
    void listResponses() throws Exception {
        String token = registerAndLogin("owner@example.com");
        Long formId = createForm(token, "만족도 조사");
        Long textId = createQuestion(token, formId,
                new QuestionRequest(QuestionType.SHORT_TEXT, "의견", true, 1, null, null, null));
        createQuestion(token, formId,
                new QuestionRequest(QuestionType.SHORT_TEXT, "추가 의견", false, 2, null, null, null));
        String slug = slugOf(token, formId);
        changeStatus(token, formId, "PUBLISHED");
        submit(slug, new AnswerRequest(textId, null, "좋았습니다", null, null, null));

        mockMvc.perform(get("/api/forms/" + formId + "/responses")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].answeredCount").value(1))
                .andExpect(jsonPath("$.content[0].totalQuestions").value(2))
                .andExpect(jsonPath("$.content[0].submittedAt").isNotEmpty());
    }

    @Test
    @DisplayName("응답 상세 → 200, 체크박스는 한 문항으로 묶이고 무응답 문항도 포함")
    void responseDetail() throws Exception {
        String token = registerAndLogin("owner@example.com");
        Long formId = createForm(token, "설문");
        Long multiId = createQuestion(token, formId, new QuestionRequest(QuestionType.MULTIPLE_CHOICE,
                "관심 분야", true, 1, null, null,
                List.of(new OptionRequest("가", 1), new OptionRequest("나", 2), new OptionRequest("다", 3))));
        createQuestion(token, formId,
                new QuestionRequest(QuestionType.SHORT_TEXT, "건너뛸 문항", false, 2, null, null, null));
        List<Long> optionIds = optionIdsOf(token, formId, multiId);
        String slug = slugOf(token, formId);
        changeStatus(token, formId, "PUBLISHED");
        Long responseId = submit(slug,
                new AnswerRequest(multiId, List.of(optionIds.get(0), optionIds.get(2)), null, null, null, null));

        mockMvc.perform(get("/api/forms/" + formId + "/responses/" + responseId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answers.length()").value(2))
                .andExpect(jsonPath("$.answers[0].answered").value(true))
                .andExpect(jsonPath("$.answers[0].selectedOptions.length()").value(2))
                .andExpect(jsonPath("$.answers[0].selectedOptions[0].label").value("가"))
                .andExpect(jsonPath("$.answers[0].selectedOptions[1].label").value("다"))
                .andExpect(jsonPath("$.answers[1].answered").value(false))
                .andExpect(jsonPath("$.answers[1].selectedOptions").isEmpty());
    }

    @Test
    @DisplayName("집계 → 200, 완료율·선택지 분포·평점 평균이 실제 질의로도 정의대로 나온다")
    void summary() throws Exception {
        String token = registerAndLogin("owner@example.com");
        Long formId = createForm(token, "만족도 조사");
        Long choiceId = createQuestion(token, formId, new QuestionRequest(QuestionType.SINGLE_CHOICE,
                "만족하시나요?", true, 1, null, null,
                List.of(new OptionRequest("만족", 1), new OptionRequest("불만족", 2))));
        Long ratingId = createQuestion(token, formId,
                new QuestionRequest(QuestionType.RATING, "추천 점수", true, 2, 1, 5, null));
        createQuestion(token, formId,
                new QuestionRequest(QuestionType.SHORT_TEXT, "의견", false, 3, null, null, null));
        List<Long> optionIds = optionIdsOf(token, formId, choiceId);
        String slug = slugOf(token, formId);
        changeStatus(token, formId, "PUBLISHED");

        // 두 응답 모두 3문항 중 2문항만 답합니다 → 완료율 2/3
        submit(slug, new AnswerRequest(choiceId, List.of(optionIds.getFirst()), null, null, null, null),
                new AnswerRequest(ratingId, null, null, 5, null, null));
        submit(slug, new AnswerRequest(choiceId, List.of(optionIds.getFirst()), null, null, null, null),
                new AnswerRequest(ratingId, null, null, 3, null, null));

        mockMvc.perform(get("/api/forms/" + formId + "/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalResponses").value(2))
                .andExpect(jsonPath("$.completionRate").value(2.0 / 3))
                .andExpect(jsonPath("$.responsesByDate.length()").value(1))
                .andExpect(jsonPath("$.responsesByDate[0].count").value(2))
                .andExpect(jsonPath("$.questionSummaries.length()").value(3))
                .andExpect(jsonPath("$.questionSummaries[0].answeredCount").value(2))
                .andExpect(jsonPath("$.questionSummaries[0].optionCounts[0].label").value("만족"))
                .andExpect(jsonPath("$.questionSummaries[0].optionCounts[0].count").value(2))
                // 아무도 고르지 않은 선택지도 0 으로 남습니다
                .andExpect(jsonPath("$.questionSummaries[0].optionCounts[1].label").value("불만족"))
                .andExpect(jsonPath("$.questionSummaries[0].optionCounts[1].count").value(0))
                .andExpect(jsonPath("$.questionSummaries[1].average").value(4.0))
                .andExpect(jsonPath("$.questionSummaries[1].valueCounts.length()").value(2))
                // 아무도 답하지 않은 문항은 0 이고 평균은 없습니다
                .andExpect(jsonPath("$.questionSummaries[2].answeredCount").value(0))
                .andExpect(jsonPath("$.questionSummaries[2].average").doesNotExist())
                .andExpect(jsonPath("$.questionSummaries[2].recentTexts").isEmpty());
    }

    @Test
    @DisplayName("체크박스로 선택지를 여러 개 골라도 완료율은 1.0 을 넘지 않는다")
    void completionRateNeverExceedsOne() throws Exception {
        String token = registerAndLogin("owner@example.com");
        Long formId = createForm(token, "설문");
        Long multiId = createQuestion(token, formId, new QuestionRequest(QuestionType.MULTIPLE_CHOICE,
                "관심 분야", true, 1, null, null,
                List.of(new OptionRequest("가", 1), new OptionRequest("나", 2), new OptionRequest("다", 3))));
        List<Long> optionIds = optionIdsOf(token, formId, multiId);
        String slug = slugOf(token, formId);
        changeStatus(token, formId, "PUBLISHED");
        submit(slug, new AnswerRequest(multiId, optionIds, null, null, null, null));

        mockMvc.perform(get("/api/forms/" + formId + "/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completionRate").value(1.0))
                .andExpect(jsonPath("$.questionSummaries[0].answeredCount").value(1));
    }

    @Test
    @DisplayName("응답 삭제 → 204, 목록과 집계에서 즉시 사라진다")
    void deleteResponse() throws Exception {
        String token = registerAndLogin("owner@example.com");
        Long formId = createForm(token, "설문");
        Long textId = createQuestion(token, formId,
                new QuestionRequest(QuestionType.SHORT_TEXT, "의견", true, 1, null, null, null));
        String slug = slugOf(token, formId);
        changeStatus(token, formId, "PUBLISHED");
        Long responseId = submit(slug, new AnswerRequest(textId, null, "지울 응답", null, null, null));

        mockMvc.perform(delete("/api/forms/" + formId + "/responses/" + responseId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/forms/" + formId + "/responses")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/forms/" + formId + "/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.totalResponses").value(0))
                .andExpect(jsonPath("$.completionRate").value(0.0));
    }

    @Test
    @DisplayName("남의 폼 응답은 볼 수 없다 → 403")
    void rejectsNonOwner() throws Exception {
        String owner = registerAndLogin("owner@example.com");
        Long formId = createForm(owner, "남의 설문");
        String intruder = registerAndLogin("intruder@example.com");

        mockMvc.perform(get("/api/forms/" + formId + "/responses")
                        .header("Authorization", "Bearer " + intruder))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        mockMvc.perform(get("/api/forms/" + formId + "/summary")
                        .header("Authorization", "Bearer " + intruder))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("토큰 없이 응답을 볼 수 없다 → 401 (익명은 제출만 가능)")
    void rejectsAnonymous() throws Exception {
        String token = registerAndLogin("owner@example.com");
        Long formId = createForm(token, "설문");

        mockMvc.perform(get("/api/forms/" + formId + "/responses"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("다른 폼의 응답 id → 404 RESPONSE_NOT_FOUND")
    void rejectsResponseOfAnotherForm() throws Exception {
        String token = registerAndLogin("owner@example.com");
        Long formId = createForm(token, "설문 A");
        Long otherFormId = createForm(token, "설문 B");
        Long textId = createQuestion(token, otherFormId,
                new QuestionRequest(QuestionType.SHORT_TEXT, "의견", true, 1, null, null, null));
        String otherSlug = slugOf(token, otherFormId);
        changeStatus(token, otherFormId, "PUBLISHED");
        Long responseId = submit(otherSlug, new AnswerRequest(textId, null, "B 의 응답", null, null, null));

        mockMvc.perform(get("/api/forms/" + formId + "/responses/" + responseId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESPONSE_NOT_FOUND"));
    }

    @Test
    @DisplayName("발행 전 폼은 일별 추이가 비어 있다")
    void draftFormHasEmptyTrend() throws Exception {
        String token = registerAndLogin("owner@example.com");
        Long formId = createForm(token, "작성 중");

        mockMvc.perform(get("/api/forms/" + formId + "/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responsesByDate").isEmpty())
                .andExpect(jsonPath("$.totalResponses").value(0));
    }

    // --- helpers ---

    private String registerAndLogin(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest(email, "password1234", "제작자"))))
                .andExpect(status().isCreated());
        String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest(email, "password1234"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(body, TokenResponse.class).accessToken();
    }

    private Long createForm(String token, String title) throws Exception {
        String body = mockMvc.perform(post("/api/forms").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", title, "description", "설명"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(body, FormDetailResponse.class).id();
    }

    private Long createQuestion(String token, Long formId, QuestionRequest request) throws Exception {
        String body = mockMvc.perform(post("/api/forms/" + formId + "/questions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(body, QuestionResponse.class).id();
    }

    private List<Long> optionIdsOf(String token, Long formId, Long questionId) throws Exception {
        return detailOf(token, formId).questions().stream()
                .filter(question -> question.id().equals(questionId))
                .findFirst().orElseThrow()
                .options().stream().map(option -> option.id()).toList();
    }

    private String slugOf(String token, Long formId) throws Exception {
        return detailOf(token, formId).slug();
    }

    private FormDetailResponse detailOf(String token, Long formId) throws Exception {
        String body = mockMvc.perform(get("/api/forms/" + formId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(body, FormDetailResponse.class);
    }

    private void changeStatus(String token, Long formId, String status) throws Exception {
        mockMvc.perform(patch("/api/forms/" + formId + "/status").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + status + "\"}"))
                .andExpect(status().isOk());
    }

    private Long submit(String slug, AnswerRequest... answers) throws Exception {
        String body = mockMvc.perform(post("/api/public/forms/" + slug + "/responses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new SubmitResponseRequest(List.of(answers)))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(body, SubmitResponseResult.class).responseId();
    }

    private String json(Object value) {
        return objectMapper.writeValueAsString(value);
    }
}
