package com.example.ams.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.ams.common.WrongQuestionNosJson;
import com.example.ams.domain.clazz.TestScore;

@Repository
public class TestScoreRepository {

	private final JdbcTemplate jdbcTemplate;

	public TestScoreRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	private TestScore mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
		return new TestScore(
				rs.getLong("score_id"),
				rs.getLong("test_id"),
				rs.getLong("student_id"),
				rs.getBigDecimal("raw_score"),
				rs.getString("grade"),
				rs.getBigDecimal("class_avg"),
				rs.getObject("rank") != null ? rs.getInt("rank") : null,
				rs.getObject("upper_rank_pct") != null ? rs.getInt("upper_rank_pct") : null,
				rs.getObject("percentile_rank") != null ? rs.getInt("percentile_rank") : null,
				null,
				rs.getObject("correct_count") != null ? rs.getInt("correct_count") : null,
				WrongQuestionNosJson.fromJson(rs.getString("wrong_question_nos")),
				rs.getTimestamp("graded_at") != null ? rs.getTimestamp("graded_at").toInstant() : null);
	}

	public List<TestScore> findByTestId(long testId) {
		return jdbcTemplate.query(
				"""
						SELECT s.*, t.question_count
						FROM test_score s
						INNER JOIN test t ON s.test_id = t.test_id
						WHERE s.test_id = ?
						ORDER BY s.student_id
						""",
				this::mapRow,
				testId);
	}

	public Optional<TestScore> findByTestIdAndStudentId(long testId, long studentId) {
		return jdbcTemplate.query(
				"""
						SELECT s.*, t.question_count
						FROM test_score s
						INNER JOIN test t ON s.test_id = t.test_id
						WHERE s.test_id = ? AND s.student_id = ?
						""",
				this::mapRow,
				testId,
				studentId).stream().findFirst();
	}

	public void insertEmpty(long testId, long studentId) {
		String sql = "INSERT INTO test_score (test_id, student_id) VALUES (?, ?)";
		jdbcTemplate.update(sql, testId, studentId);
	}

	public void updateScore(
			long testId,
			long studentId,
			BigDecimal rawScore,
			String grade,
			BigDecimal classAvg,
			Integer rank) {
		if (findByTestIdAndStudentId(testId, studentId).isEmpty()) {
			insertEmpty(testId, studentId);
		}
		jdbcTemplate.update(
				"""
						UPDATE test_score
						SET raw_score = ?, grade = ?, class_avg = ?, `rank` = ?,
						    upper_rank_pct = NULL, percentile_rank = NULL
						WHERE test_id = ? AND student_id = ?
						""",
				rawScore,
				grade,
				classAvg,
				rank,
				testId,
				studentId);
	}

	public void updateRankAndClassAvg(long testId, long studentId, BigDecimal classAvg, Integer rank) {
		jdbcTemplate.update(
				"""
						UPDATE test_score
						SET class_avg = ?, `rank` = ?,
						    upper_rank_pct = NULL, percentile_rank = NULL
						WHERE test_id = ? AND student_id = ?
						""",
				classAvg,
				rank,
				testId,
				studentId);
	}

	public TestScore upsertGraded(
			long testId,
			long studentId,
			int correctCount,
			List<Integer> wrongQuestionNos,
			BigDecimal rawScore,
			Instant gradedAt) {
		if (findByTestIdAndStudentId(testId, studentId).isEmpty()) {
			insertEmpty(testId, studentId);
		}
		jdbcTemplate.update(
				"""
						UPDATE test_score
						SET answers = NULL, correct_count = ?, wrong_question_nos = ?,
						    raw_score = ?, graded_at = ?
						WHERE test_id = ? AND student_id = ?
						""",
				correctCount,
				WrongQuestionNosJson.toJson(wrongQuestionNos),
				rawScore,
				gradedAt != null ? java.sql.Timestamp.from(gradedAt) : null,
				testId,
				studentId);
		return findByTestIdAndStudentId(testId, studentId).orElseThrow();
	}

	public boolean hasGradedScore(long testId) {
		Integer count = jdbcTemplate.queryForObject(
				"""
						SELECT COUNT(*) FROM test_score
						WHERE test_id = ?
						  AND (raw_score IS NOT NULL OR correct_count IS NOT NULL OR graded_at IS NOT NULL)
						""",
				Integer.class,
				testId);
		return count != null && count > 0;
	}

	public void deleteByTestId(long testId) {
		jdbcTemplate.update("DELETE FROM test_score WHERE test_id = ?", testId);
	}
}
