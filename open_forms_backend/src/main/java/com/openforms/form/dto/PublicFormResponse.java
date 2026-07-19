package com.openforms.form.dto;

import com.openforms.form.domain.Form;
import com.openforms.form.domain.FormStatus;
import java.util.List;

/**
 * 공개 링크(slug)로 조회한 폼입니다. 소유자 정보·감사 컬럼 등 제작자 전용 정보는 담지 않습니다.
 *
 * <p>{@code status} 를 포함하는 이유는 <b>종료된 폼도 조회는 허용</b>하기 때문입니다. 응답 화면이 이 값을 보고
 * 입력 폼 대신 "종료된 설문입니다" 안내를 렌더링합니다(제출은 409 로 막힙니다).
 */
public record PublicFormResponse(String slug, String title, String description, FormStatus status,
        List<PublicQuestionResponse> questions) {

    public static PublicFormResponse of(Form form, List<PublicQuestionResponse> questions) {
        return new PublicFormResponse(form.getSlug(), form.getTitle(), form.getDescription(),
                form.getStatus(), questions);
    }
}
