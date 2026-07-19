package com.openforms.form.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.openforms.form.domain.QuestionType;
import com.openforms.form.dto.FormDetailResponse;
import com.openforms.form.dto.OptionRequest;
import com.openforms.form.dto.QuestionRequest;
import com.openforms.form.dto.QuestionResponse;
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
 * 질문 nested CRUD 를 실제 보안 체인·필터와 함께 검증합니다. 소유권(403)·중첩 정합(404)·발행 폼 편집(409)·
 * 타입별 검증(400)과 삭제 후 폼 상세 연동까지 확인합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class QuestionControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("단답 질문 추가 → 201 + Location + 선택지 없음")
    void createShortText() throws Exception {
        String token = registerAndLogin("owner@example.com", "제작자");
        Long formId = createForm(token, "설문");

        mockMvc.perform(post("/api/forms/" + formId + "/questions").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new QuestionRequest(QuestionType.SHORT_TEXT, "의견을 남겨주세요", true, 1,
                                null, null, null))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.matchesRegex("/api/forms/" + formId + "/questions/\\d+")))
                .andExpect(jsonPath("$.type").value("SHORT_TEXT"))
                .andExpect(jsonPath("$.options").isEmpty());
    }

    @Test
    @DisplayName("선택형 질문 추가(선택지 3) → 201 + 선택지 id 발급")
    void createSingleChoice() throws Exception {
        String token = registerAndLogin("owner@example.com", "제작자");
        Long formId = createForm(token, "설문");

        mockMvc.perform(post("/api/forms/" + formId + "/questions").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new QuestionRequest(QuestionType.SINGLE_CHOICE, "만족하시나요?", true, 1,
                                null, null, List.of(new OptionRequest("매우 만족", 1),
                                        new OptionRequest("보통", 2), new OptionRequest("불만족", 3))))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.options.length()").value(3))
                .andExpect(jsonPath("$.options[0].id").isNumber())
                .andExpect(jsonPath("$.options[0].label").value("매우 만족"));
    }

    @Test
    @DisplayName("선택형 선택지 1개 → 400 OPTIONS_REQUIRED")
    void createChoiceTooFewOptions() throws Exception {
        String token = registerAndLogin("owner@example.com", "제작자");
        Long formId = createForm(token, "설문");

        mockMvc.perform(post("/api/forms/" + formId + "/questions").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new QuestionRequest(QuestionType.DROPDOWN, "선택", true, 1, null, null,
                                List.of(new OptionRequest("유일", 1))))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("OPTIONS_REQUIRED"));
    }

    @Test
    @DisplayName("RATING minValue>maxValue → 400 INVALID_VALUE_RANGE")
    void createRatingInvertedRange() throws Exception {
        String token = registerAndLogin("owner@example.com", "제작자");
        Long formId = createForm(token, "설문");

        mockMvc.perform(post("/api/forms/" + formId + "/questions").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new QuestionRequest(QuestionType.RATING, "점수", true, 1, 5, 1, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_VALUE_RANGE"));
    }

    @Test
    @DisplayName("제목 공백 → 400 VALIDATION_FAILED")
    void createBlankTitle() throws Exception {
        String token = registerAndLogin("owner@example.com", "제작자");
        Long formId = createForm(token, "설문");

        mockMvc.perform(post("/api/forms/" + formId + "/questions").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new QuestionRequest(QuestionType.SHORT_TEXT, "", true, 1, null, null, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("타인 폼에 질문 추가 → 403")
    void createOnOthersForm() throws Exception {
        String owner = registerAndLogin("owner@example.com", "소유자");
        String other = registerAndLogin("other@example.com", "타인");
        Long formId = createForm(owner, "내 폼");

        mockMvc.perform(post("/api/forms/" + formId + "/questions").header("Authorization", "Bearer " + other)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new QuestionRequest(QuestionType.SHORT_TEXT, "질문", true, 1, null, null, null))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("없는 폼에 질문 추가 → 404 FORM_NOT_FOUND")
    void createOnMissingForm() throws Exception {
        String token = registerAndLogin("owner@example.com", "제작자");

        mockMvc.perform(post("/api/forms/99999999/questions").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new QuestionRequest(QuestionType.SHORT_TEXT, "질문", true, 1, null, null, null))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FORM_NOT_FOUND"));
    }

    @Test
    @DisplayName("토큰 없이 질문 추가 → 401")
    void createWithoutToken() throws Exception {
        mockMvc.perform(post("/api/forms/1/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new QuestionRequest(QuestionType.SHORT_TEXT, "질문", true, 1, null, null, null))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("발행된 폼에 질문 추가 → 409 FORM_NOT_EDITABLE")
    void createOnPublishedForm() throws Exception {
        String token = registerAndLogin("owner@example.com", "제작자");
        Long formId = createForm(token, "설문");
        changeStatus(token, formId, "PUBLISHED");

        mockMvc.perform(post("/api/forms/" + formId + "/questions").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new QuestionRequest(QuestionType.SHORT_TEXT, "질문", true, 1, null, null, null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FORM_NOT_EDITABLE"));
    }

    @Test
    @DisplayName("질문 수정 → 200, 제목·선택지 교체 반영")
    void updateQuestion() throws Exception {
        String token = registerAndLogin("owner@example.com", "제작자");
        Long formId = createForm(token, "설문");
        Long questionId = createQuestion(token, formId, new QuestionRequest(QuestionType.SINGLE_CHOICE,
                "이전 질문", true, 1, null, null, List.of(new OptionRequest("A", 1), new OptionRequest("B", 2))));

        mockMvc.perform(put("/api/forms/" + formId + "/questions/" + questionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new QuestionRequest(QuestionType.SINGLE_CHOICE, "바뀐 질문", false, 1,
                                null, null, List.of(new OptionRequest("가", 1), new OptionRequest("나", 2),
                                        new OptionRequest("다", 3))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("바뀐 질문"))
                .andExpect(jsonPath("$.options.length()").value(3))
                .andExpect(jsonPath("$.options[0].label").value("가"));
    }

    @Test
    @DisplayName("폼에 속하지 않은 질문 수정 → 404 QUESTION_NOT_FOUND")
    void updateMissingQuestion() throws Exception {
        String token = registerAndLogin("owner@example.com", "제작자");
        Long formId = createForm(token, "설문");

        mockMvc.perform(put("/api/forms/" + formId + "/questions/99999999")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new QuestionRequest(QuestionType.SHORT_TEXT, "질문", true, 1, null, null, null))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("QUESTION_NOT_FOUND"));
    }

    @Test
    @DisplayName("질문 삭제 → 204, 폼 상세에서 사라진다")
    void deleteQuestion() throws Exception {
        String token = registerAndLogin("owner@example.com", "제작자");
        Long formId = createForm(token, "설문");
        Long questionId = createQuestion(token, formId, new QuestionRequest(QuestionType.SHORT_TEXT,
                "삭제될 질문", true, 1, null, null, null));

        mockMvc.perform(delete("/api/forms/" + formId + "/questions/" + questionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/forms/" + formId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions").isEmpty());
    }

    @Test
    @DisplayName("질문 삭제 실패 경로: 타인 403 · 다른 폼의 질문 404 · 발행 폼 409")
    void deleteQuestionFailures() throws Exception {
        String owner = registerAndLogin("owner@example.com", "소유자");
        String other = registerAndLogin("other@example.com", "타인");
        Long formId = createForm(owner, "설문");
        Long questionId = createQuestion(owner, formId, new QuestionRequest(QuestionType.SHORT_TEXT,
                "질문", true, 1, null, null, null));

        mockMvc.perform(delete("/api/forms/" + formId + "/questions/" + questionId)
                        .header("Authorization", "Bearer " + other))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(delete("/api/forms/" + formId + "/questions/99999999")
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("QUESTION_NOT_FOUND"));

        // 발행 후에는 이미 수집된 응답과 어긋나므로 삭제도 막힙니다(추가·수정과 같은 규칙).
        changeStatus(owner, formId, "PUBLISHED");
        mockMvc.perform(delete("/api/forms/" + formId + "/questions/" + questionId)
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FORM_NOT_EDITABLE"));

        mockMvc.perform(get("/api/forms/" + formId).header("Authorization", "Bearer " + owner))
                .andExpect(jsonPath("$.questions.length()").value(1));
    }

    // --- helpers ---

    private String registerAndLogin(String email, String name) throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest(email, "password1234", name))))
                .andExpect(status().isCreated());
        String loginBody = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest(email, "password1234"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(loginBody, TokenResponse.class).accessToken();
    }

    private Long createForm(String token, String title) throws Exception {
        String responseBody = mockMvc.perform(post("/api/forms").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(java.util.Map.of("title", title, "description", "설명"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(responseBody, FormDetailResponse.class).id();
    }

    private Long createQuestion(String token, Long formId, QuestionRequest request) throws Exception {
        String responseBody = mockMvc.perform(post("/api/forms/" + formId + "/questions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(responseBody, QuestionResponse.class).id();
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
