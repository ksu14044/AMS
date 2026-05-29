package com.example.ams.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * V15 Flyway 미적용 환경(핫 리로드 등)에서 NULL 삽입 오류 방지.
 * homework_rate 등이 이미 NULL 허용이면 no-op.
 */
@Component
@Order(100)
public class DiligenceReportSchemaPatch implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DiligenceReportSchemaPatch.class);

	private final JdbcTemplate jdbcTemplate;

	public DiligenceReportSchemaPatch(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (!tableExists()) {
			return;
		}
		if (isNullable("homework_rate")) {
			return;
		}
		log.warn("diligence_report: V15 스키마 보정 실행 (rate/grade NULL 허용)");
		jdbcTemplate.execute("""
				ALTER TABLE diligence_report
				    MODIFY homework_rate INT NULL,
				    MODIFY clinic_rate INT NULL,
				    MODIFY video_rate INT NULL,
				    MODIFY homework_grade CHAR(1) NULL,
				    MODIFY clinic_grade CHAR(1) NULL,
				    MODIFY video_grade CHAR(1) NULL
				""");
	}

	private boolean tableExists() {
		Integer count = jdbcTemplate.queryForObject(
				"""
						SELECT COUNT(*) FROM information_schema.tables
						WHERE table_schema = DATABASE() AND table_name = 'diligence_report'
						""",
				Integer.class);
		return count != null && count > 0;
	}

	private boolean isNullable(String columnName) {
		String nullable = jdbcTemplate.queryForObject(
				"""
						SELECT IS_NULLABLE FROM information_schema.columns
						WHERE table_schema = DATABASE()
						  AND table_name = 'diligence_report'
						  AND column_name = ?
						""",
				String.class,
				columnName);
		return "YES".equalsIgnoreCase(nullable);
	}
}
