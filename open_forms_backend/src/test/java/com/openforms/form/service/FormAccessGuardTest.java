package com.openforms.form.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.openforms.common.exception.ResourceNotFoundException;
import com.openforms.form.domain.Form;
import com.openforms.form.repository.FormRepository;
import com.openforms.user.domain.User;
import com.openforms.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

/**
 * 폼 소유권 가드가 존재(404)와 소유(403)를 이 순서로 구분하는지 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
class FormAccessGuardTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private FormRepository formRepository;
    @InjectMocks
    private FormAccessGuard guard;

    @Test
    @DisplayName("폼이 없으면 404(FORM_NOT_FOUND)")
    void missingFormNotFound() {
        User caller = org.mockito.Mockito.mock(User.class);
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(caller));
        when(formRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guard.requireOwnedForm(1L, "owner@example.com"))
                .asInstanceOf(InstanceOfAssertFactories.type(ResourceNotFoundException.class))
                .satisfies(ex -> assertThat(ex.getCode()).isEqualTo("FORM_NOT_FOUND"));
    }

    @Test
    @DisplayName("남의 폼이면 403(AccessDeniedException)")
    void nonOwnerForbidden() {
        UUID ownerId = UUID.randomUUID();
        UUID callerId = UUID.randomUUID();
        User caller = org.mockito.Mockito.mock(User.class);
        when(caller.getId()).thenReturn(callerId);
        when(userRepository.findByEmail("intruder@example.com")).thenReturn(Optional.of(caller));

        User owner = org.mockito.Mockito.mock(User.class);
        when(owner.getId()).thenReturn(ownerId);
        Form form = org.mockito.Mockito.mock(Form.class);
        when(form.getUser()).thenReturn(owner);
        when(formRepository.findById(1L)).thenReturn(Optional.of(form));

        assertThatThrownBy(() -> guard.requireOwnedForm(1L, "intruder@example.com"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("소유한 폼이면 폼을 반환한다")
    void ownerReturnsForm() {
        UUID ownerId = UUID.randomUUID();
        User owner = org.mockito.Mockito.mock(User.class);
        when(owner.getId()).thenReturn(ownerId);
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));

        Form form = org.mockito.Mockito.mock(Form.class);
        when(form.getUser()).thenReturn(owner);
        when(formRepository.findById(1L)).thenReturn(Optional.of(form));

        assertThat(guard.requireOwnedForm(1L, "owner@example.com")).isSameAs(form);
    }
}
