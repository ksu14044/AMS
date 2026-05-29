package com.example.ams.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.example.ams.domain.clazz.ClinicWeek;
import com.example.ams.domain.clazz.ClinicWeekStatus;

@Repository
public class ClinicWeekRepository {

	private static final RowMapper<ClinicWeek> ROW_MAPPER = (rs, rowNum) -> new ClinicWeek(
			rs.getLong("class_id"),
			rs.getDate("week_start_date").toLocalDate(),
			ClinicWeekStatus.valueOf(rs.getString("status")),
			rs.getTimestamp("locked_at") != null ? rs.getTimestamp("locked_at").toInstant() : null);

	private final JdbcTemplate jdbcTemplate;

	public ClinicWeekRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Optional<ClinicWeek> findByClassIdAndWeekStart(long classId, LocalDate weekStart) {
		return jdbcTemplate.query(
				"SELECT * FROM clinic_week WHERE class_id = ? AND week_start_date = ?",
				ROW_MAPPER,
				classId,
				weekStart).stream().findFirst();
	}

	public ClinicWeek insertOpen(long classId, LocalDate weekStart) {
		jdbcTemplate.update(
				"INSERT INTO clinic_week (class_id, week_start_date, status) VALUES (?, ?, 'OPEN')",
				classId,
				weekStart);
		return findByClassIdAndWeekStart(classId, weekStart).orElseThrow();
	}

	public ClinicWeek ensureOpenWeek(long classId, LocalDate weekStart) {
		return findByClassIdAndWeekStart(classId, weekStart).orElseGet(() -> insertOpen(classId, weekStart));
	}

	public void lockWeek(long classId, LocalDate weekStart) {
		jdbcTemplate.update(
				"""
						UPDATE clinic_week SET status = 'LOCKED', locked_at = CURRENT_TIMESTAMP
						WHERE class_id = ? AND week_start_date = ?
						""",
				classId,
				weekStart);
	}

	public List<ClinicWeek> findOpenByWeekStart(LocalDate weekStart) {
		return jdbcTemplate.query(
				"SELECT * FROM clinic_week WHERE week_start_date = ? AND status = 'OPEN'",
				ROW_MAPPER,
				weekStart);
	}
}
