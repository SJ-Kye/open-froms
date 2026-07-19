package com.openforms.form.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.openforms.common.exception.ConflictException;
import com.openforms.common.response.PageResponse;
import com.openforms.form.domain.Form;
import com.openforms.form.domain.FormStatus;
import com.openforms.form.dto.CreateFormRequest;
import com.openforms.form.dto.FormDetailResponse;
import com.openforms.form.dto.FormStatusResponse;
import com.openforms.form.dto.FormSummaryResponse;
import com.openforms.form.repository.FormRepository;
import com.openforms.form.repository.FormRepository.ResponseCountRow;
import com.openforms.user.domain.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * 폼 서비스 규칙을 하이브리드 TDD 로 먼저 고정합니다: 생성 시 DRAFT·slug·소유자 세팅, 무효 상태 전이 409,
 * 삭제 위임, 목록 응답 수 매핑.
 */
@ExtendWith(MockitoExtension.class)
class FormServiceTest {

    @Mock
    private FormRepository formRepository;
    @Mock
    private QuestionLoader questionLoader;
    @Mock
    private FormAccessGuard accessGuard;
    @Mock
    private SlugGenerator slugGenerator;
    @InjectMocks
    private FormService formService;

    @Test
    @DisplayName("폼 생성 시 상태 DRAFT·slug·소유자가 세팅되어 저장된다")
    void createSetsDraftSlugOwner() {
        User owner = mock(User.class);
        when(accessGuard.currentUser("owner@example.com")).thenReturn(owner);
        when(slugGenerator.generate()).thenReturn("slug1234");
        when(formRepository.findBySlug("slug1234")).thenReturn(Optional.empty());
        when(formRepository.save(any(Form.class))).thenAnswer(inv -> inv.getArgument(0));

        FormDetailResponse response = formService.create("owner@example.com",
                new CreateFormRequest("고객 만족도 조사", "설명"));

        ArgumentCaptor<Form> saved = ArgumentCaptor.forClass(Form.class);
        verify(formRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(FormStatus.DRAFT);
        assertThat(saved.getValue().getSlug()).isEqualTo("slug1234");
        assertThat(saved.getValue().getUser()).isSameAs(owner);
        assertThat(saved.getValue().getTitle()).isEqualTo("고객 만족도 조사");
        assertThat(response.title()).isEqualTo("고객 만족도 조사");
        assertThat(response.status()).isEqualTo(FormStatus.DRAFT);
        assertThat(response.questions()).isEmpty();
    }

    @Test
    @DisplayName("무효한 상태 전이는 409(INVALID_STATUS_TRANSITION)")
    void changeStatusInvalidTransition() {
        Form form = new Form(mock(User.class), "제목", "설명", "slug1234"); // DRAFT
        when(accessGuard.requireOwnedForm(1L, "owner@example.com")).thenReturn(form);

        assertThatThrownBy(() -> formService.changeStatus("owner@example.com", 1L, FormStatus.CLOSED))
                .asInstanceOf(InstanceOfAssertFactories.type(ConflictException.class))
                .satisfies(ex -> assertThat(ex.getCode()).isEqualTo("INVALID_STATUS_TRANSITION"));
    }

    @Test
    @DisplayName("유효한 상태 전이(DRAFT→PUBLISHED)는 적용된다")
    void changeStatusValid() {
        Form form = new Form(mock(User.class), "제목", "설명", "slug1234"); // DRAFT
        when(accessGuard.requireOwnedForm(1L, "owner@example.com")).thenReturn(form);

        FormStatusResponse response = formService.changeStatus("owner@example.com", 1L, FormStatus.PUBLISHED);

        assertThat(response.status()).isEqualTo(FormStatus.PUBLISHED);
    }

    @Test
    @DisplayName("삭제는 소유 확인 후 레포지토리에 위임한다(DB 캐스케이드)")
    void deleteDelegatesToRepository() {
        Form form = mock(Form.class);
        when(accessGuard.requireOwnedForm(1L, "owner@example.com")).thenReturn(form);

        formService.delete("owner@example.com", 1L);

        verify(formRepository).delete(form);
    }

    @Test
    @DisplayName("목록은 폼별 응답 수를 매핑한다")
    void listMapsResponseCount() {
        UUID ownerId = UUID.randomUUID();
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(ownerId);
        when(accessGuard.currentUser("owner@example.com")).thenReturn(owner);

        Pageable pageable = PageRequest.of(0, 20);
        Form form = mock(Form.class);
        when(form.getId()).thenReturn(10L);
        when(form.getTitle()).thenReturn("고객 만족도 조사");
        when(form.getStatus()).thenReturn(FormStatus.PUBLISHED);
        when(form.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(formRepository.findByUser_Id(ownerId, pageable))
                .thenReturn(new PageImpl<>(List.of(form), pageable, 1));

        ResponseCountRow row = mock(ResponseCountRow.class);
        when(row.getFormId()).thenReturn(10L);
        when(row.getCnt()).thenReturn(3L);
        when(formRepository.countResponsesByFormIds(List.of(10L))).thenReturn(List.of(row));

        PageResponse<FormSummaryResponse> response = formService.list("owner@example.com", null, pageable);

        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content()).singleElement()
                .satisfies(item -> assertThat(item.responseCount()).isEqualTo(3L));
    }
}
