package com.openforms.form.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.openforms.common.exception.ConflictException;
import com.openforms.user.domain.User;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 폼이 생명주기 시각(발행·종료)을 스스로 기록하는지 검증합니다. 일별 응답 추이 차트가 "발행일부터 오늘까지"
 * 를 그리려면 발행 시점이 필요한데, 감사 컬럼 {@code updatedAt} 은 이후 제목 수정에도 바뀌므로 발행 시각의
 * 근거가 될 수 없습니다. 그래서 상태 전이가 일어난 시각을 전이 시점에 못박습니다.
 */
class FormLifecycleTest {

    @Test
    @DisplayName("생성 직후에는 발행·종료 시각이 없습니다")
    void draftHasNoLifecycleTimestamps() {
        Form form = newForm();

        assertThat(form.getStatus()).isEqualTo(FormStatus.DRAFT);
        assertThat(form.getPublishedAt()).isNull();
        assertThat(form.getClosedAt()).isNull();
    }

    @Test
    @DisplayName("발행하면 publishedAt 이 기록되고 closedAt 은 그대로 비어 있습니다")
    void publishRecordsPublishedAt() {
        Form form = newForm();
        LocalDateTime before = LocalDateTime.now();

        form.changeStatus(FormStatus.PUBLISHED);

        assertThat(form.getPublishedAt()).isNotNull().isAfterOrEqualTo(before);
        assertThat(form.getClosedAt()).isNull();
    }

    @Test
    @DisplayName("종료하면 closedAt 이 기록되고 publishedAt 은 발행 당시 값을 유지합니다")
    void closeRecordsClosedAtAndKeepsPublishedAt() {
        Form form = newForm();
        form.changeStatus(FormStatus.PUBLISHED);
        LocalDateTime publishedAt = form.getPublishedAt();

        form.changeStatus(FormStatus.CLOSED);

        assertThat(form.getPublishedAt()).isEqualTo(publishedAt);
        assertThat(form.getClosedAt()).isNotNull().isAfterOrEqualTo(publishedAt);
    }

    @Test
    @DisplayName("허용되지 않는 전이는 409 이며 시각도 남기지 않습니다")
    void rejectedTransitionLeavesNoTimestamp() {
        Form form = newForm();

        assertThatThrownBy(() -> form.changeStatus(FormStatus.CLOSED))
                .isInstanceOf(ConflictException.class);

        assertThat(form.getStatus()).isEqualTo(FormStatus.DRAFT);
        assertThat(form.getPublishedAt()).isNull();
        assertThat(form.getClosedAt()).isNull();
    }

    private Form newForm() {
        return new Form(new User("owner@example.com", "hash", "제작자"), "설문", "설명", "abcd1234");
    }
}
