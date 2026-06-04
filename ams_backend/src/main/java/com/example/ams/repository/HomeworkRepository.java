package com.example.ams.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.example.ams.domain.clazz.AssignmentStatus;
import com.example.ams.domain.clazz.Homework;

@Repository
public class HomeworkRepository {

	private static final RowMapper<Homework> ROW_MAPPER = (rs, rowNum) -> new Homework(
			rs.getLong("homework_id"),
			rs.getLong("class_id"),
			rs.getString("title"),
			rs.getObject("question_count") != null ? rs.getInt("question_count") : null,
			rs.getString("answer_key_pdf_path"),
			AssignmentStatus.valueOf(rs.getString("status")),
			rs.getTimestamp("created_at").toInstant());

	private final JdbcTemplate jdbcTemplate;

	public HomeworkRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Optional<Homework> findById(long homeworkId) {
		return jdbcTemplate.query("SELECT * FROM homework WHERE homework_id = ?", ROW_MAPPER, homeworkId)
				.stream()
				.findFirst();
	}

	public List<Homework> findByClassId(long classId) {
		return jdbcTemplate.query(
				"SELECT * FROM homework WHERE class_id = ? ORDER BY created_at DESC",
				ROW_MAPPER,
				classId);
	}

	public Homework insert(long classId, String title, Integer questionCount, AssignmentStatus status) {
		return insert(classId, null, title, questionCount, status);
	}

	public Homework insert(
			long classId,
			Long lessonRecordId,
			String title,
			Integer questionCount,
			AssignmentStatus status) {
		String sql = "INSERT INTO homework (class_id, lesson_record_id, title, question_count, status) VALUES (?, ?, ?, ?, ?)";
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(connection -> {
			var ps = connection.prepareStatement(sql, new String[] { "homework_id" });
			ps.setLong(1, classId);
			if (lessonRecordId != null) {
				ps.setLong(2, lessonRecordId);
			} else {
				ps.setNull(2, java.sql.Types.BIGINT);
			}
			ps.setString(3, title);
			if (questionCount != null) {
				ps.setInt(4, questionCount);
			} else {
				ps.setNull(4, java.sql.Types.INTEGER);
			}
			ps.setString(5, status.name());
			return ps;
		}, keyHolder);
		return findById(keyHolder.getKey().longValue()).orElseThrow();
	}

	public void updateQuestionCount(long homeworkId, int questionCount) {
		jdbcTemplate.update("UPDATE homework SET question_count = ? WHERE homework_id = ?", questionCount, homeworkId);
	}

	public void updateAnswerKeyPdfPath(long homeworkId, String path) {
		jdbcTemplate.update("UPDATE homework SET answer_key_pdf_path = ? WHERE homework_id = ?", path, homeworkId);
	}

	public void updateMetadata(long homeworkId, String title, Integer questionCount) {
		jdbcTemplate.update(
				"UPDATE homework SET title = ?, question_count = ? WHERE homework_id = ?",
				title,
				questionCount,
				homeworkId);
	}

	public Long findLessonRecordId(long homeworkId) {
		return jdbcTemplate.query(
				"SELECT lesson_record_id FROM homework WHERE homework_id = ?",
				rs -> {
					if (!rs.next()) {
						return null;
					}
					long value = rs.getLong("lesson_record_id");
					return rs.wasNull() ? null : value;
				},
				homeworkId);
	}

	public void deleteById(long homeworkId) {
		jdbcTemplate.update("DELETE FROM homework WHERE homework_id = ?", homeworkId);
	}

	public void updateStatus(long homeworkId, AssignmentStatus status) {
		jdbcTemplate.update("UPDATE homework SET status = ? WHERE homework_id = ?", status.name(), homeworkId);
	}

	public List<HomeworkSummary> findSummariesByLessonRecordId(long lessonRecordId) {
		return jdbcTemplate.query(
				"""
						SELECT homework_id, title, question_count, status
						FROM homework WHERE lesson_record_id = ? ORDER BY homework_id
						""",
				(rs, rowNum) -> new HomeworkSummary(
						rs.getLong("homework_id"),
						rs.getString("title"),
						rs.getObject("question_count") != null ? rs.getInt("question_count") : null,
						AssignmentStatus.valueOf(rs.getString("status"))),
				lessonRecordId);
	}

	public record HomeworkSummary(long homeworkId, String title, Integer questionCount, AssignmentStatus status) {
	}
}
