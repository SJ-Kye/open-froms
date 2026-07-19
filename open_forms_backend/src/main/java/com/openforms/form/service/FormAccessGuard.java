package com.openforms.form.service;

import com.openforms.common.exception.ResourceNotFoundException;
import com.openforms.form.domain.Form;
import com.openforms.form.repository.FormRepository;
import com.openforms.user.domain.User;
import com.openforms.user.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * 폼 소유권 검사를 한곳에 모읍니다. 인증 주체(이메일)를 소유자 UUID 로 해석하고, 대상 폼의 존재(404)와
 * 소유(403)를 <b>이 순서로</b> 구분해 검증합니다. {@link FormService} 와 이후 질문 서비스가 공유합니다.
 *
 * <p>404 를 먼저, 403 을 나중에 판단합니다 — 존재하지 않는 폼을 403 으로 답하면 리소스 존재 여부가
 * 뒤바뀌어 노출되므로, 없으면 404, 있으나 남의 것이면 403 으로 명확히 갈립니다.
 */
@Component
public class FormAccessGuard {

    private final UserRepository userRepository;
    private final FormRepository formRepository;

    public FormAccessGuard(UserRepository userRepository, FormRepository formRepository) {
        this.userRepository = userRepository;
        this.formRepository = formRepository;
    }

    /** 인증 주체(이메일)에 해당하는 사용자입니다. 없으면 404(토큰은 유효하나 계정이 사라진 희귀 케이스). */
    public User currentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));
    }

    /** 존재(404)·소유(403)를 검증하고 소유한 폼을 반환합니다. */
    public Form requireOwnedForm(Long formId, String email) {
        User user = currentUser(email);
        Form form = formRepository.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException("FORM_NOT_FOUND", "폼을 찾을 수 없습니다."));
        if (!form.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("접근 권한이 없습니다.");
        }
        return form;
    }
}
