package com.openforms.form.repository;

import com.openforms.form.domain.Form;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FormRepository extends JpaRepository<Form, Long> {

    Optional<Form> findBySlug(String slug);
}
