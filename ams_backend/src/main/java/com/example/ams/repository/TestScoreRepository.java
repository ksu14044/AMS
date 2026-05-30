package com.example.ams.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.ams.common.HomeworkAnswersJson;
import com.example.ams.domain.clazz.TestScore;

@Repository
public class TestScoreRepository {

	private final JdbcTemplate jdbcTemplate;

	public TestScoreRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	private TestScore mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
		String answersJson = rs.getString("answers");
		Integer questionCount = rs.getObject("question_count") != null
				? rs.getInt("question_count")
				: null;
		int count = questionCount != null ? questionCount : 0;
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
				HomeworkAnswersJson.fromJson(answersJson, count),
				rs.getObject("correct_count") != null ? rs.getInt("correct_count") : null,
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
			List<String> answers,
			int correctCount,
			BigDecimal rawScore,
			Instant gradedAt) {
		if (findByTestIdAndStudentId(testId, studentId).isEmpty()) {
			insertEmpty(testId, studentId);
		}
		jdbcTemplate.update(
				"""
						UPDATE test_score
						SET answers = ?, correct_count = ?, raw_score = ?, graded_at = ?
						WHERE test_id = ? AND student_id = ?
						""",
				HomeworkAnswersJson.toJson(answers),
				correctCount,
				rawScore,
				gradedAt != null ? java.sql.Timestamp.from(gradedAt) : null,
				testId,
				studentId);
		return findByTestIdAndStudentId(testId, studentId).orElseThrow();
	}
}
