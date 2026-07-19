package com.openforms.form.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.openforms.form.domain.QuestionType;
import com.openforms.form.dto.FormDetailResponse;
import com.openforms.form.dto.OptionRequest;
import com.openforms.form.dto.QuestionRequest;
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
 * 공개 폼 조회를 검증합니다. 핵심은 <b>토큰 없이</b> 열리는 것과, 발행 상태에 따른 노출 기준
 * (DRAFT 404 · PUBLISHED/CLOSED 200)입니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PublicFormControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("발행 폼은 토큰 없이 200 — 질문·선택지가 노출된다")
    void getPublishedFormAnonymously() throws Exception {
        String token = registerAndLogin("owner@example.com");
        Long formId = createForm(token, "고객 만족도 조사");
        createQuestion(token, formId, new QuestionRequest(QuestionType.SINGLE_CHOICE, "만족하시나요?", true, 1,
                null, null, List.of(new OptionRequest("만족", 1), new OptionRequest("불만족", 2))));
        createQuestion(token, formId, new QuestionRequest(QuestionType.RATING, "추천 점수", false, 2, 1, 5, null));
        String slug = slugOf(token, formId);
        changeStatus(token, formId, "PUBLISHED");

        // Authorization 헤더 없이 호출
        mockMvc.perform(get("/api/public/forms/" + slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value(slug))
                .andExpect(jsonPath("$.title").value("고객 만족도 조사"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.questions.length()").value(2))
                .andExpect(jsonPath("$.questions[0].options.length()").value(2))
                .andExpect(jsonPath("$.questions[0].options[0].label").value("만족"))
                .andExpect(jsonPath("$.questions[1].minValue").value(1))
                .andExpect(jsonPath("$.questions[1].maxValue").value(5));
    }

    @Test
    @DisplayName("미발행(DRAFT) 폼은 404 — 존재를 흘리지 않는다")
    void draftFormIsNotFound() throws Exception {
        String token = registerAndLogin("owner@example.com");
        Long formId = createForm(token, "작성 중인 폼");
        String slug = slugOf(token, formId);

        mockMvc.perform(get("/api/public/forms/" + slug))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FORM_NOT_FOUND"));
    }

    @Test
    @DisplayName("종료(CLOSED) 폼은 200 + status=CLOSED — 종료 안내를 위해 조회는 허용")
    void closedFormIsStillReadable() throws Exception {
        String token = registerAndLogin("owner@example.com");
        Long formId = createForm(token, "종료된 설문");
        String slug = slugOf(token, formId);
        changeStatus(token, formId, "PUBLISHED");
        changeStatus(token, formId, "CLOSED");

        mockMvc.perform(get("/api/public/forms/" + slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    @DisplayName("없는 slug → 404")
    void unknownSlugIsNotFound() throws Exception {
        mockMvc.perform(get("/api/public/forms/nosuchslug"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FORM_NOT_FOUND"));
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

    private String slugOf(String token, Long formId) throws Exception {
        String body = mockMvc.perform(get("/api/forms/" + formId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(body, FormDetailResponse.class).slug();
    }

    private void createQuestion(String token, Long formId, QuestionRequest request) throws Exception {
        mockMvc.perform(post("/api/forms/" + formId + "/questions").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isCreated());
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
