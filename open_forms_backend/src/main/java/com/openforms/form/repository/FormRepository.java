package com.openforms.form.repository;

import com.openforms.form.domain.Form;
import com.openforms.form.domain.FormStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FormRepository extends JpaRepository<Form, Long> {

    Optional<Form> findBySlug(String slug);

    /** 소유자별 폼 목록(페이지). */
    Page<Form> findByUser_Id(UUID userId, Pageable pageable);

    /** 소유자 + 상태 필터 폼 목록(페이지). */
    Page<Form> findByUser_IdAndStatus(UUID userId, FormStatus status, Pageable pageable);

    /**
     * 폼별 응답 수를 한 번에 집계합니다. 네이티브 쿼리로 {@code responses} 테이블만 참조하므로
     * {@code form} 이 {@code response} 자바 타입에 의존하지 않아 모듈 경계가 유지됩니다. 호출부는
     * {@code formIds} 가 비면 이 메서드를 호출하지 않습니다({@code IN ()} 문법 오류 방지).
     */
    @Query(value = "SELECT form_id AS formId, COUNT(*) AS cnt FROM responses "
            + "WHERE form_id IN (:formIds) GROUP BY form_id", nativeQuery = true)
    List<ResponseCountRow> countResponsesByFormIds(Collection<Long> formIds);

    /** 응답 수 집계 결과 투영입니다. */
    interface ResponseCountRow {
        Long getFormId();

        long getCnt();
    }
}
