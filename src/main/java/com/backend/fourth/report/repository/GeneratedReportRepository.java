package com.backend.fourth.report.repository;

import com.backend.fourth.report.entity.GeneratedReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GeneratedReportRepository extends JpaRepository<GeneratedReport, Long> {
}
