package com.openforms.response.controller;

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
import com.openforms.user.dto.LoginRequest;
import com.openforms.user.dto.RegisterRequest;
import com.openforms.user.dto.TokenResponse;
import java.util.List;
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
 * 익명 응답 제출을 실제 보안 체인과 함께 검증합니다. 폼 생성·질문 추가·발행까지 제작자 API 로 준비한 뒤,
 * <b>토큰 없이</b> 제출합니다. 제출 결과가 제작자 쪽 responseCount 에 반영되는지도 함께 확인합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PublicResponseControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("정상 제출 → 201 + responseId·submittedAt, 제작자 목록의 responseCount 증가")
    void submitAnonymously() throws Exception {
        String token = registerAndLogin("owner@example.com");
        Long formId = createForm(token, "고객 만족도 조사");
        Long textId = createQuestion(token, formId,
                new QuestionRequest(QuestionType.SHORT_TEXT, "의견", true, 1, null, null, null));
        Long multiId = createQuestion(token, formId, new QuestionRequest(QuestionType.MULTIPLE_CHOICE,
                "관심 분야", true, 2, null, null,
                List.of(new OptionRequest("가", 1), new OptionRequest("나", 2), new OptionRequest("다", 3))));
        List<Long> optionIds = optionIdsOf(token, formId, multiId);
        String slug = slugOf(token, formId);
        changeStatus(token, formId, "PUBLISHED");

        mockMvc.perform(post("/api/public/forms/" + slug + "/responses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new SubmitResponseRequest(List.of(
                                new AnswerRequest(textId, null, "친절했습니다", null, null, null),
                                new AnswerRequest(multiId, List.of(optionIds.get(0), optionIds.get(2)),
                                        null, null, null, null))))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.responseId").isNumber())
                .andExpect(jsonPath("$.submittedAt").isNotEmpty());

        mockMvc.perform(get("/api/forms").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].responseCount").value(1));
    }

    @Test
    @DisplayName("필수 질문 누락 → 400 REQUIRED_ANSWER_MISSING + fieldErrors")
    void missingRequiredAnswer() throws Exception {
        String token = registerAndLogin("owner@example.com");
        Long formId = createForm(token, "설문");
        Long requiredId = createQuestion(token, formId,
                new QuestionRequest(QuestionType.SHORT_TEXT, "필수 의견", true, 1, null, null, null));
        Long optionalId = createQuestion(token, formId,
                new QuestionRequest(QuestionType.SHORT_TEXT, "선택 의견", false, 2, null, null, null));
        String slug = slugOf(token, formId);
        changeStatus(token, formId, "PUBLISHED");

        mockMvc.perform(post("/api/public/forms/" + slug + "/responses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new SubmitResponseRequest(List.of(
                                new AnswerRequest(optionalId, null, "선택만 답함", null, null, null))))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUIRED_ANSWER_MISSING"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("questionId:" + requiredId));
    }

    @Test
    @DisplayName("종료된 폼에 제출 → 409 FORM_CLOSED")
    void submitToClosedForm() throws Exception {
        String token = registerAndLogin("owner@example.com");
        Long formId = createForm(token, "종료 설문");
        Long questionId = createQuestion(token, formId,
                new QuestionRequest(QuestionType.SHORT_TEXT, "의견", false, 1, null, null, null));
        String slug = slugOf(token, formId);
        changeStatus(token, formId, "PUBLISHED");
        changeStatus(token, formId, "CLOSED");

        mockMvc.perform(post("/api/public/forms/" + slug + "/responses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new SubmitResponseRequest(List.of(
                                new AnswerRequest(questionId, null, "늦은 응답", null, null, null))))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FORM_CLOSED"));
    }

    @Test
    @DisplayName("미발행 폼에 제출 → 404 FORM_NOT_FOUND")
    void submitToDraftForm() throws Exception {
        String token = registerAndLogin("owner@example.com");
        Long formId = createForm(token, "작성 중");
        Long questionId = createQuestion(token, formId,
                new QuestionRequest(QuestionType.SHORT_TEXT, "의견", false, 1, null, null, null));
        String slug = slugOf(token, formId);

        mockMvc.perform(post("/api/public/forms/" + slug + "/responses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new SubmitResponseRequest(List.of(
                                new AnswerRequest(questionId, null, "미리 응답", null, null, null))))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FORM_NOT_FOUND"));
    }

    @Test
    @DisplayName("RATING 범위 밖 → 400 ANSWER_OUT_OF_RANGE")
    void outOfRangeRating() throws Exception {
        String token = registerAndLogin("owner@example.com");
        Long formId = createForm(token, "평점 설문");
        Long ratingId = createQuestion(token, formId,
                new QuestionRequest(QuestionType.RATING, "추천 점수", true, 1, 1, 5, null));
        String slug = slugOf(token, formId);
        changeStatus(token, formId, "PUBLISHED");

        mockMvc.perform(post("/api/public/forms/" + slug + "/responses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new SubmitResponseRequest(List.of(
                                new AnswerRequest(ratingId, null, null, 9, null, null))))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ANSWER_OUT_OF_RANGE"));
    }

    @Test
    @DisplayName("answers 빈 배열 → 400 VALIDATION_FAILED")
    void emptyAnswers() throws Exception {
        String token = registerAndLogin("owner@example.com");
        Long formId = createForm(token, "설문");
        String slug = slugOf(token, formId);
        changeStatus(token, formId, "PUBLISHED");

        mockMvc.perform(post("/api/public/forms/" + slug + "/responses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("이 폼에 없는 질문 id → 400 UNKNOWN_QUESTION")
    void unknownQuestion() throws Exception {
        String token = registerAndLogin("owner@example.com");
        Long formId = createForm(token, "설문");
        createQuestion(token, formId,
                new QuestionRequest(QuestionType.SHORT_TEXT, "의견", false, 1, null, null, null));
        String slug = slugOf(token, formId);
        changeStatus(token, formId, "PUBLISHED");

        mockMvc.perform(post("/api/public/forms/" + slug + "/responses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new SubmitResponseRequest(List.of(
                                new AnswerRequest(99999999L, null, "엉뚱한 질문", null, null, null))))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNKNOWN_QUESTION"));
    }

    @Test
    @DisplayName("같은 질문에 두 번 응답 → 400 DUPLICATE_ANSWER")
    void duplicateAnswer() throws Exception {
        String token = registerAndLogin("owner@example.com");
        Long formId = createForm(token, "설문");
        Long questionId = createQuestion(token, formId,
                new QuestionRequest(QuestionType.SHORT_TEXT, "의견", false, 1, null, null, null));
        String slug = slugOf(token, formId);
        changeStatus(token, formId, "PUBLISHED");

        mockMvc.perform(post("/api/public/forms/" + slug + "/responses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new SubmitResponseRequest(List.of(
                                new AnswerRequest(questionId, null, "첫 번째", null, null, null),
                                new AnswerRequest(questionId, null, "두 번째", null, null, null))))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DUPLICATE_ANSWER"));
    }

    @Test
    @DisplayName("택1 질문에 선택지 2개 → 400 INVALID_ANSWER_VALUE")
    void invalidAnswerValue() throws Exception {
        String token = registerAndLogin("owner@example.com");
        Long formId = createForm(token, "설문");
        Long choiceId = createQuestion(token, formId, new QuestionRequest(QuestionType.SINGLE_CHOICE,
                "하나만 고르세요", true, 1, null, null,
                List.of(new OptionRequest("가", 1), new OptionRequest("나", 2))));
        List<Long> optionIds = optionIdsOf(token, formId, choiceId);
        String slug = slugOf(token, formId);
        changeStatus(token, formId, "PUBLISHED");

        mockMvc.perform(post("/api/public/forms/" + slug + "/responses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new SubmitResponseRequest(List.of(
                                new AnswerRequest(choiceId, optionIds, null, null, null, null))))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ANSWER_VALUE"));
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
                        .content(json(java.util.Map.of("title", title, "description", "설명"))))
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
        String body = mockMvc.perform(get("/api/forms/" + formId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(body, FormDetailResponse.class).questions().stream()
                .filter(q -> q.id().equals(questionId))
                .findFirst().orElseThrow()
                .options().stream().map(o -> o.id()).toList();
    }

    private String slugOf(String token, Long formId) throws Exception {
        String body = mockMvc.perform(get("/api/forms/" + formId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(body, FormDetailResponse.class).slug();
    }

    private void changeStatus(String token, Long formId, String status) throws Exception {
        mockMvc.perform(patch("/api/forms/" + formId + "/status").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + status + "\"}"))
                .andExpect(status().isOk());
    }

    private String json(Object value) {
        return objectMapper.writeValueAsString(value);
    }
}
