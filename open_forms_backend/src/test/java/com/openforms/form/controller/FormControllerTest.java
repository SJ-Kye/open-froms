package com.openforms.form.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.openforms.form.dto.FormDetailResponse;
import com.openforms.user.dto.LoginRequest;
import com.openforms.user.dto.RegisterRequest;
import com.openforms.user.dto.TokenResponse;
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
 * 폼 CRUD 를 실제 보안 체인·필터와 함께 검증합니다. 두 사용자를 등록/로그인해 소유권(403)을 확인합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FormControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("폼 생성 → 201 + Location + DRAFT·slug")
    void createForm() throws Exception {
        String token = registerAndLogin("owner@example.com", "제작자");

        mockMvc.perform(post("/api/forms").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body("고객 만족도 조사", "서비스 개선 설문"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/forms/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.slug").isNotEmpty())
                .andExpect(jsonPath("$.questions").isEmpty());
    }

    @Test
    @DisplayName("제목 공백 생성 → 400 VALIDATION_FAILED")
    void createBlankTitle() throws Exception {
        String token = registerAndLogin("owner@example.com", "제작자");

        mockMvc.perform(post("/api/forms").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body("", "설명"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors").isNotEmpty());
    }

    @Test
    @DisplayName("토큰 없이 폼 생성 → 401")
    void createWithoutToken() throws Exception {
        mockMvc.perform(post("/api/forms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body("제목", "설명"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("목록 → 페이지 envelope + 상태 필터, responseCount=0")
    void listWithPaginationAndFilter() throws Exception {
        String token = registerAndLogin("owner@example.com", "제작자");
        Long published = createForm(token, "발행폼", "설명");
        createForm(token, "초안폼", "설명");
        changeStatus(token, published, "PUBLISHED");

        mockMvc.perform(get("/api/forms").header("Authorization", "Bearer " + token)
                        .param("page", "0").param("size", "10").param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].responseCount").value(0));

        mockMvc.perform(get("/api/forms").header("Authorization", "Bearer " + token)
                        .param("status", "PUBLISHED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("PUBLISHED"));
    }

    @Test
    @DisplayName("상세: 소유 200 · 타인 403 · 없음 404")
    void getDetailOwnershipAndMissing() throws Exception {
        String owner = registerAndLogin("owner@example.com", "소유자");
        String other = registerAndLogin("other@example.com", "타인");
        Long id = createForm(owner, "내 폼", "설명");

        mockMvc.perform(get("/api/forms/" + id).header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        mockMvc.perform(get("/api/forms/" + id).header("Authorization", "Bearer " + other))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(get("/api/forms/99999999").header("Authorization", "Bearer " + owner))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FORM_NOT_FOUND"));
    }

    @Test
    @DisplayName("수정 → 200 제목 반영")
    void updateForm() throws Exception {
        String token = registerAndLogin("owner@example.com", "제작자");
        Long id = createForm(token, "원래 제목", "설명");

        mockMvc.perform(put("/api/forms/" + id).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body("바뀐 제목", "새 설명"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("바뀐 제목"));
    }

    @Test
    @DisplayName("상태 변경: DRAFT→PUBLISHED 200, 무효 전이 409")
    void patchStatus() throws Exception {
        String token = registerAndLogin("owner@example.com", "제작자");
        Long id = createForm(token, "폼", "설명");

        mockMvc.perform(patch("/api/forms/" + id + "/status").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        // PUBLISHED→DRAFT 는 허용되지 않는 역방향 전이
        mockMvc.perform(patch("/api/forms/" + id + "/status").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DRAFT\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    @DisplayName("삭제 → 204 후 조회 404")
    void deleteForm() throws Exception {
        String token = registerAndLogin("owner@example.com", "제작자");
        Long id = createForm(token, "삭제될 폼", "설명");

        mockMvc.perform(delete("/api/forms/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/forms/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
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

    private Long createForm(String token, String title, String description) throws Exception {
        String responseBody = mockMvc.perform(post("/api/forms").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(json(body(title, description))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(responseBody, FormDetailResponse.class).id();
    }

    private void changeStatus(String token, Long id, String status) throws Exception {
        mockMvc.perform(patch("/api/forms/" + id + "/status").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + status + "\"}"))
                .andExpect(status().isOk());
    }

    private java.util.Map<String, String> body(String title, String description) {
        return java.util.Map.of("title", title, "description", description);
    }

    private String json(Object value) {
        return objectMapper.writeValueAsString(value);
    }
}
