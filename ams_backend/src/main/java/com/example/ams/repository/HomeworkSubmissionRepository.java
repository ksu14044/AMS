package com.example.ams.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.example.ams.domain.clazz.HomeworkSubmission;

@Repository
public class HomeworkSubmissionRepository {

	private static final RowMapper<HomeworkSubmission> ROW_MAPPER = (rs, rowNum) -> new HomeworkSubmission(
			rs.getLong("submission_id"),
			rs.getLong("homework_id"),
			rs.getLong("student_id"),
			rs.getBoolean("submitted"),
			rs.getTimestamp("submitted_at") != null ? rs.getTimestamp("submitted_at").toInstant() : null,
			rs.getBigDecimal("score"),
			rs.getString("grade"),
			rs.getString("memo"));

	private final JdbcTemplate jdbcTemplate;

	public HomeworkSubmissionRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<HomeworkSubmission> findByHomeworkId(long homeworkId) {
		return jdbcTemplate.query(
				"SELECT * FROM homework_submission WHERE homework_id = ? ORDER BY student_id",
				ROW_MAPPER,
				homeworkId);
	}

	public Optional<HomeworkSubmission> findByHomeworkIdAndStudentId(long homeworkId, long studentId) {
		return jdbcTemplate.query(
				"SELECT * FROM homework_submission WHERE homework_id = ? AND student_id = ?",
				ROW_MAPPER,
				homeworkId,
				studentId).stream().findFirst();
	}

	public HomeworkSubmission insertEmpty(long homeworkId, long studentId) {
		String sql = "INSERT INTO homework_submission (homework_id, student_id) VALUES (?, ?)";
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(connection -> {
			var ps = connection.prepareStatement(sql, new String[] { "submission_id" });
			ps.setLong(1, homeworkId);
			ps.setLong(2, studentId);
			return ps;
		}, keyHolder);
		return findById(keyHolder.getKey().longValue()).orElseThrow();
	}

	public Optional<HomeworkSubmission> findById(long submissionId) {
		return jdbcTemplate.query(
				"SELECT * FROM homework_submission WHERE submission_id = ?",
				ROW_MAPPER,
				submissionId).stream().findFirst();
	}

	public HomeworkSubmission upsert(
			long homeworkId,
			long studentId,
			boolean submitted,
			Instant submittedAt,
			BigDecimal score,
			String grade,
			String memo) {
		Optional<HomeworkSubmission> existing = findByHomeworkIdAndStudentId(homeworkId, studentId);
		if (existing.isEmpty()) {
			insertEmpty(homeworkId, studentId);
		}
		jdbcTemplate.update(
				"""
						UPDATE homework_submission
						SET submitted = ?, submitted_at = ?, score = ?, grade = ?, memo = ?
						WHERE homework_id = ? AND student_id = ?
						""",
				submitted,
				submittedAt != null ? java.sql.Timestamp.from(submittedAt) : null,
				score,
				grade,
				memo,
				homeworkId,
				studentId);
		return findByHomeworkIdAndStudentId(homeworkId, studentId).orElseThrow();
	}
}
