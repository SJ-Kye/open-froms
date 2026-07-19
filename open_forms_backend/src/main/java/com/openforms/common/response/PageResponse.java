package com.openforms.common.response;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * 목록 API 의 공통 페이지 응답 래퍼입니다. 성공 본문은 bare 를 유지하는 규약이지만, 페이지네이션 메타
 * (전체 개수·페이지 수)는 본문에 함께 실어야 클라이언트가 페이지 이동을 구성할 수 있어 이 형태를 씁니다.
 *
 * @param content       현재 페이지의 항목들입니다.
 * @param page          0-기반 페이지 번호입니다.
 * @param size          페이지 크기입니다.
 * @param totalElements 조건에 맞는 전체 항목 수입니다.
 * @param totalPages    전체 페이지 수입니다.
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    /** Spring Data {@link Page} 를 응답 래퍼로 변환합니다. */
    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
