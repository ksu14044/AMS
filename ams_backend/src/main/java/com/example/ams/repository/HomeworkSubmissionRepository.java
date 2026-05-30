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

import com.example.ams.common.HomeworkAnswersJson;
import com.example.ams.domain.clazz.HomeworkSubmission;

@Repository
public class HomeworkSubmissionRepository {

	private final JdbcTemplate jdbcTemplate;

	public HomeworkSubmissionRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	private HomeworkSubmission mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
		String answersJson = rs.getString("answers");
		Integer questionCount = rs.getObject("question_count") != null
				? rs.getInt("question_count")
				: null;
		int count = questionCount != null ? questionCount : 0;
		return new HomeworkSubmission(
				rs.getLong("submission_id"),
				rs.getLong("homework_id"),
				rs.getLong("student_id"),
				rs.getBoolean("submitted"),
				rs.getTimestamp("submitted_at") != null ? rs.getTimestamp("submitted_at").toInstant() : null,
				rs.getBigDecimal("score"),
				rs.getString("grade"),
				rs.getString("memo"),
				HomeworkAnswersJson.fromJson(answersJson, count),
				rs.getObject("correct_count") != null ? rs.getInt("correct_count") : null,
				rs.getTimestamp("completed_at") != null ? rs.getTimestamp("completed_at").toInstant() : null);
	}

	public List<HomeworkSubmission> findByHomeworkId(long homeworkId) {
		return jdbcTemplate.query(
				"""
						SELECT s.*, h.question_count
						FROM homework_submission s
						INNER JOIN homework h ON s.homework_id = h.homework_id
						WHERE s.homework_id = ?
						ORDER BY s.student_id
						""",
				this::mapRow,
				homeworkId);
	}

	public Optional<HomeworkSubmission> findByHomeworkIdAndStudentId(long homeworkId, long studentId) {
		return jdbcTemplate.query(
				"""
						SELECT s.*, h.question_count
						FROM homework_submission s
						INNER JOIN homework h ON s.homework_id = h.homework_id
						WHERE s.homework_id = ? AND s.student_id = ?
						""",
				this::mapRow,
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
				"""
						SELECT s.*, h.question_count
						FROM homework_submission s
						INNER JOIN homework h ON s.homework_id = h.homework_id
						WHERE s.submission_id = ?
						""",
				this::mapRow,
				submissionId).stream().findFirst();
	}

	public HomeworkSubmission upsertLegacy(
			long homeworkId,
			long studentId,
			boolean submitted,
			Instant submittedAt,
			BigDecimal score,
			String grade,
			String memo) {
		if (findByHomeworkIdAndStudentId(homeworkId, studentId).isEmpty()) {
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

	public HomeworkSubmission upsertGraded(
			long homeworkId,
			long studentId,
			List<String> answers,
			int correctCount,
			BigDecimal score,
			Instant completedAt) {
		if (findByHomeworkIdAndStudentId(homeworkId, studentId).isEmpty()) {
			insertEmpty(homeworkId, studentId);
		}
		jdbcTemplate.update(
				"""
						UPDATE homework_submission
						SET answers = ?, correct_count = ?, score = ?, submitted = ?, completed_at = ?
						WHERE homework_id = ? AND student_id = ?
						""",
				HomeworkAnswersJson.toJson(answers),
				correctCount,
				score,
				score != null,
				completedAt != null ? java.sql.Timestamp.from(completedAt) : null,
				homeworkId,
				studentId);
		return findByHomeworkIdAndStudentId(homeworkId, studentId).orElseThrow();
	}
}
