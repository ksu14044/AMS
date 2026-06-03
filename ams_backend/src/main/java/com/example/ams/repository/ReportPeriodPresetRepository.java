package com.example.ams.repository;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.example.ams.domain.report.ReportPeriodPreset;

@Repository
public class ReportPeriodPresetRepository {

	private static final RowMapper<ReportPeriodPreset> ROW_MAPPER = (rs, rowNum) -> new ReportPeriodPreset(
			rs.getLong("preset_id"),
			rs.getLong("class_id"),
			rs.getString("name"),
			rs.getDate("period_start").toLocalDate(),
			rs.getDate("period_end").toLocalDate());

	private final JdbcTemplate jdbcTemplate;

	public ReportPeriodPresetRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<ReportPeriodPreset> findByClassId(long classId) {
		return jdbcTemplate.query(
				"""
						SELECT * FROM report_period_preset
						WHERE class_id = ?
						ORDER BY period_start DESC, preset_id DESC
						""",
				ROW_MAPPER,
				classId);
	}

	public Optional<ReportPeriodPreset> findByIdAndClassId(long presetId, long classId) {
		return jdbcTemplate.query(
				"SELECT * FROM report_period_preset WHERE preset_id = ? AND class_id = ?",
				ROW_MAPPER,
				presetId,
				classId)
				.stream()
				.findFirst();
	}

	public ReportPeriodPreset insert(long classId, String name, java.time.LocalDate periodStart, java.time.LocalDate periodEnd) {
		String sql = """
				INSERT INTO report_period_preset (class_id, name, period_start, period_end)
				VALUES (?, ?, ?, ?)
				""";
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(connection -> {
			var ps = connection.prepareStatement(sql, new String[] { "preset_id" });
			ps.setLong(1, classId);
			ps.setString(2, name);
			ps.setDate(3, Date.valueOf(periodStart));
			ps.setDate(4, Date.valueOf(periodEnd));
			return ps;
		}, keyHolder);
		long presetId = keyHolder.getKey().longValue();
		return findByIdAndClassId(presetId, classId).orElseThrow();
	}

	public ReportPeriodPreset update(
			long presetId,
			long classId,
			String name,
			java.time.LocalDate periodStart,
			java.time.LocalDate periodEnd) {
		jdbcTemplate.update(
				"""
						UPDATE report_period_preset
						SET name = ?, period_start = ?, period_end = ?
						WHERE preset_id = ? AND class_id = ?
						""",
				name,
				Date.valueOf(periodStart),
				Date.valueOf(periodEnd),
				presetId,
				classId);
		return findByIdAndClassId(presetId, classId).orElseThrow();
	}

	public void delete(long presetId, long classId) {
		jdbcTemplate.update("DELETE FROM report_period_preset WHERE preset_id = ? AND class_id = ?", presetId, classId);
	}
}
