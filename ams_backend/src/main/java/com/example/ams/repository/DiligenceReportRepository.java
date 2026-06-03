package com.example.ams.repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.example.ams.domain.report.DiligenceReport;

@Repository
public class DiligenceReportRepository {

	private static final RowMapper<DiligenceReport> ROW_MAPPER = (rs, rowNum) -> new DiligenceReport(
			rs.getLong("report_id"),
			rs.getLong("class_id"),
			rs.getLong("student_id"),
			rs.getObject("test_id") != null ? rs.getLong("test_id") : null,
			rs.getTimestamp("period_start").toInstant(),
			rs.getTimestamp("period_end").toInstant(),
			rs.getString("period_label"),
			rs.getObject("period_preset_id") != null ? rs.getLong("period_preset_id") : null,
			rs.getInt("homework_submitted"),
			rs.getInt("homework_total"),
			rs.getObject("homework_rate") != null ? rs.getInt("homework_rate") : null,
			rs.getString("homework_grade"),
			rs.getInt("clinic_attended"),
			rs.getInt("clinic_total"),
			rs.getObject("clinic_rate") != null ? rs.getInt("clinic_rate") : null,
			rs.getString("clinic_grade"),
			rs.getBigDecimal("test_raw_score"),
			rs.getBigDecimal("test_class_avg"),
			rs.getObject("test_upper_rank_pct") != null ? rs.getInt("test_upper_rank_pct") : null,
			rs.getObject("test_percentile_rank") != null ? rs.getInt("test_percentile_rank") : null,
			rs.getObject("test_rank") != null ? rs.getInt("test_rank") : null,
			rs.getString("test_grade"),
			rs.getInt("video_certified"),
			rs.getInt("video_total"),
			rs.getObject("video_rate") != null ? rs.getInt("video_rate") : null,
			rs.getString("video_grade"),
			rs.getInt("total_score"),
			rs.getString("overall_grade"),
			rs.getString("teacher_comment"),
			rs.getString("pdf_path"),
			rs.getTimestamp("created_at").toInstant());

	private final JdbcTemplate jdbcTemplate;

	public DiligenceReportRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Optional<DiligenceReport> findById(long reportId) {
		return jdbcTemplate.query("SELECT * FROM diligence_report WHERE report_id = ?", ROW_MAPPER, reportId)
				.stream()
				.findFirst();
	}

	public List<DiligenceReport> findByClassId(long classId) {
		return jdbcTemplate.query(
				"SELECT * FROM diligence_report WHERE class_id = ? ORDER BY created_at DESC",
				ROW_MAPPER,
				classId);
	}

	public List<DiligenceReport> findByClassIdAndStudentId(long classId, long studentId) {
		return jdbcTemplate.query(
				"""
						SELECT * FROM diligence_report
						WHERE class_id = ? AND student_id = ?
						ORDER BY created_at DESC
						""",
				ROW_MAPPER,
				classId,
				studentId);
	}

	public List<DiligenceReport> findByClassIdAndTestId(long classId, long testId) {
		return jdbcTemplate.query(
				"""
						SELECT r.* FROM diligence_report r
						INNER JOIN `user` u ON r.student_id = u.user_id
						WHERE r.class_id = ? AND r.test_id = ?
						ORDER BY u.name ASC
						""",
				ROW_MAPPER,
				classId,
				testId);
	}

	public List<DiligenceReport> findByClassIdAndPeriod(long classId, Instant periodStart, Instant periodEnd) {
		return jdbcTemplate.query(
				"""
						SELECT r.* FROM diligence_report r
						INNER JOIN `user` u ON r.student_id = u.user_id
						WHERE r.class_id = ? AND r.period_start = ? AND r.period_end = ?
						ORDER BY u.name ASC
						""",
				ROW_MAPPER,
				classId,
				Timestamp.from(periodStart),
				Timestamp.from(periodEnd));
	}

	public void deleteByTestId(long testId) {
		jdbcTemplate.update("DELETE FROM diligence_report WHERE test_id = ?", testId);
	}

	public void deleteByClassIdAndPeriodAndStudentIds(
			long classId,
			Instant periodStart,
			Instant periodEnd,
			List<Long> studentIds) {
		if (studentIds.isEmpty()) {
			return;
		}
		String placeholders = String.join(",", studentIds.stream().map(id -> "?").toList());
		Object[] args = new Object[3 + studentIds.size()];
		args[0] = classId;
		args[1] = Timestamp.from(periodStart);
		args[2] = Timestamp.from(periodEnd);
		for (int i = 0; i < studentIds.size(); i++) {
			args[3 + i] = studentIds.get(i);
		}
		jdbcTemplate.update(
				"""
						DELETE FROM diligence_report
						WHERE class_id = ? AND period_start = ? AND period_end = ?
						  AND student_id IN (%s)
						""".formatted(placeholders),
				args);
	}

	public boolean existsByTestId(long testId) {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM diligence_report WHERE test_id = ?",
				Integer.class,
				testId);
		return count != null && count > 0;
	}

	public DiligenceReport insert(ReportInsert row) {
		String sql = """
				INSERT INTO diligence_report (
				  class_id, student_id, test_id, period_start, period_end,
				  period_label, period_preset_id,
				  homework_submitted, homework_total, homework_rate, homework_grade,
				  clinic_attended, clinic_total, clinic_rate, clinic_grade,
				  test_raw_score, test_class_avg, test_upper_rank_pct, test_percentile_rank, test_rank, test_grade,
				  video_certified, video_total, video_rate, video_grade,
				  total_score, overall_grade, teacher_comment, pdf_path
				) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
				""";
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(connection -> {
			var ps = connection.prepareStatement(sql, new String[] { "report_id" });
			int i = 1;
			ps.setLong(i++, row.classId());
			ps.setLong(i++, row.studentId());
			setLong(ps, i++, row.testId());
			ps.setTimestamp(i++, Timestamp.from(row.periodStart()));
			ps.setTimestamp(i++, Timestamp.from(row.periodEnd()));
			ps.setString(i++, row.periodLabel());
			setLong(ps, i++, row.periodPresetId());
			ps.setInt(i++, row.homeworkSubmitted());
			ps.setInt(i++, row.homeworkTotal());
			setInteger(ps, i++, row.homeworkRate());
			setString(ps, i++, row.homeworkGrade());
			ps.setInt(i++, row.clinicAttended());
			ps.setInt(i++, row.clinicTotal());
			setInteger(ps, i++, row.clinicRate());
			setString(ps, i++, row.clinicGrade());
			ps.setBigDecimal(i++, row.testRawScore());
			ps.setBigDecimal(i++, row.testClassAvg());
			setInteger(ps, i++, row.testUpperRankPct());
			setInteger(ps, i++, row.testPercentileRank());
			setInteger(ps, i++, row.testRank());
			ps.setString(i++, row.testGrade());
			ps.setInt(i++, row.videoCertified());
			ps.setInt(i++, row.videoTotal());
			setInteger(ps, i++, row.videoRate());
			setString(ps, i++, row.videoGrade());
			ps.setInt(i++, row.totalScore());
			ps.setString(i++, row.overallGrade());
			ps.setString(i++, row.teacherComment());
			ps.setString(i++, row.pdfPath());
			return ps;
		}, keyHolder);
		return findById(keyHolder.getKey().longValue()).orElseThrow();
	}

	public void updateComment(long reportId, String comment) {
		jdbcTemplate.update(
				"UPDATE diligence_report SET teacher_comment = ? WHERE report_id = ?",
				comment,
				reportId);
	}

	public void updatePdfPath(long reportId, String pdfPath) {
		jdbcTemplate.update("UPDATE diligence_report SET pdf_path = ? WHERE report_id = ?", pdfPath, reportId);
	}

	private static void setInteger(java.sql.PreparedStatement ps, int index, Integer value) throws java.sql.SQLException {
		if (value == null) {
			ps.setNull(index, java.sql.Types.INTEGER);
		} else {
			ps.setInt(index, value);
		}
	}

	private static void setLong(java.sql.PreparedStatement ps, int index, Long value) throws java.sql.SQLException {
		if (value == null) {
			ps.setNull(index, java.sql.Types.BIGINT);
		} else {
			ps.setLong(index, value);
		}
	}

	private static void setString(java.sql.PreparedStatement ps, int index, String value) throws java.sql.SQLException {
		if (value == null) {
			ps.setNull(index, java.sql.Types.VARCHAR);
		} else {
			ps.setString(index, value);
		}
	}

	public record ReportInsert(
			long classId,
			long studentId,
			Long testId,
			Instant periodStart,
			Instant periodEnd,
			String periodLabel,
			Long periodPresetId,
			int homeworkSubmitted,
			int homeworkTotal,
			Integer homeworkRate,
			String homeworkGrade,
			int clinicAttended,
			int clinicTotal,
			Integer clinicRate,
			String clinicGrade,
			BigDecimal testRawScore,
			BigDecimal testClassAvg,
			Integer testUpperRankPct,
			Integer testPercentileRank,
			Integer testRank,
			String testGrade,
			int videoCertified,
			int videoTotal,
			Integer videoRate,
			String videoGrade,
			int totalScore,
			String overallGrade,
			String teacherComment,
			String pdfPath) {
	}
}
