package com.example.ams.repository;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.example.ams.domain.clazz.TestAnswerKey;

@Repository
public class TestAnswerKeyRepository {

	private static final RowMapper<TestAnswerKey> ROW_MAPPER = (rs, rowNum) -> new TestAnswerKey(
			rs.getLong("test_id"),
			rs.getInt("question_no"),
			rs.getString("correct_answer"));

	private final JdbcTemplate jdbcTemplate;

	public TestAnswerKeyRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<TestAnswerKey> findByTestId(long testId) {
		return jdbcTemplate.query(
				"""
						SELECT test_id, question_no, correct_answer
						FROM test_answer_key
						WHERE test_id = ?
						ORDER BY question_no
						""",
				ROW_MAPPER,
				testId);
	}

	public void replaceAll(long testId, List<TestAnswerKey> keys) {
		jdbcTemplate.update("DELETE FROM test_answer_key WHERE test_id = ?", testId);
		for (TestAnswerKey key : keys) {
			jdbcTemplate.update(
					"""
							INSERT INTO test_answer_key (test_id, question_no, correct_answer)
							VALUES (?, ?, ?)
							""",
					testId,
					key.questionNo(),
					key.correctAnswer());
		}
	}
}
