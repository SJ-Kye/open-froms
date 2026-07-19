package com.openforms.form.domain;

/** 폼의 생명주기 상태입니다. DRAFT(작성 중) → PUBLISHED(발행) → CLOSED(종료). */
public enum FormStatus {
    DRAFT,
    PUBLISHED,
    CLOSED;

    /**
     * 이 상태에서 {@code target} 으로의 전이가 허용되는지 반환합니다. 선형 forward-only 규칙으로,
     * {@code DRAFT→PUBLISHED} 와 {@code PUBLISHED→CLOSED} 만 허용하고 역방향·건너뛰기·동일 상태는 막습니다.
     */
    public boolean canTransitionTo(FormStatus target) {
        return (this == DRAFT && target == PUBLISHED)
                || (this == PUBLISHED && target == CLOSED);
    }
}
