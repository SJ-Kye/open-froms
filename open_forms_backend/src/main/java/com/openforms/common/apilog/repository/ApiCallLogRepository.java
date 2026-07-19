package com.openforms.common.apilog.repository;

import com.openforms.common.apilog.domain.ApiCallLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiCallLogRepository extends JpaRepository<ApiCallLog, Long> {
}
