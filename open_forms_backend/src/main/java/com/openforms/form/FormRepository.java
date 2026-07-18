package com.openforms.form;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FormRepository extends JpaRepository<Form, Long> {

    Optional<Form> findBySlug(String slug);
}
