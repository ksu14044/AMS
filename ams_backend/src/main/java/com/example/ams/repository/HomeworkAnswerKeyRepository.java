package com.example.ams.repository;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.example.ams.domain.clazz.HomeworkAnswerKey;

@Repository
public class HomeworkAnswerKeyRepository {

	private static final RowMapper<HomeworkAnswerKey> ROW_MAPPER = (rs, rowNum) -> new HomeworkAnswerKey(
			rs.getLong("homework_id"),
			rs.getInt("question_no"),
			rs.getString("correct_answer"));

	private final JdbcTemplate jdbcTemplate;

	public HomeworkAnswerKeyRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<HomeworkAnswerKey> findByHomeworkId(long homeworkId) {
		return jdbcTemplate.query(
				"""
						SELECT homework_id, question_no, correct_answer
						FROM homework_answer_key
						WHERE homework_id = ?
						ORDER BY question_no
						""",
				ROW_MAPPER,
				homeworkId);
	}

	public void replaceAll(long homeworkId, List<HomeworkAnswerKey> keys) {
		jdbcTemplate.update("DELETE FROM homework_answer_key WHERE homework_id = ?", homeworkId);
		for (HomeworkAnswerKey key : keys) {
			jdbcTemplate.update(
					"""
							INSERT INTO homework_answer_key (homework_id, question_no, correct_answer)
							VALUES (?, ?, ?)
							""",
					homeworkId,
					key.questionNo(),
					key.correctAnswer());
		}
	}
}
