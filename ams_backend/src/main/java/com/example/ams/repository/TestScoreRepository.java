package com.example.ams.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.example.ams.domain.clazz.TestScore;

@Repository
public class TestScoreRepository {

	private static final RowMapper<TestScore> ROW_MAPPER = (rs, rowNum) -> new TestScore(
			rs.getLong("score_id"),
			rs.getLong("test_id"),
			rs.getLong("student_id"),
			rs.getBigDecimal("raw_score"),
			rs.getString("grade"),
			rs.getBigDecimal("class_avg"),
			rs.getObject("upper_rank_pct") != null ? rs.getInt("upper_rank_pct") : null,
			rs.getObject("percentile_rank") != null ? rs.getInt("percentile_rank") : null);

	private final JdbcTemplate jdbcTemplate;

	public TestScoreRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<TestScore> findByTestId(long testId) {
		return jdbcTemplate.query(
				"SELECT * FROM test_score WHERE test_id = ? ORDER BY student_id",
				ROW_MAPPER,
				testId);
	}

	public Optional<TestScore> findByTestIdAndStudentId(long testId, long studentId) {
		return jdbcTemplate.query(
				"SELECT * FROM test_score WHERE test_id = ? AND student_id = ?",
				ROW_MAPPER,
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
			Integer upperRankPct,
			Integer percentileRank) {
		Optional<TestScore> existing = findByTestIdAndStudentId(testId, studentId);
		if (existing.isEmpty()) {
			insertEmpty(testId, studentId);
		}
		jdbcTemplate.update(
				"""
						UPDATE test_score
						SET raw_score = ?, grade = ?, class_avg = ?, upper_rank_pct = ?, percentile_rank = ?
						WHERE test_id = ? AND student_id = ?
						""",
				rawScore,
				grade,
				classAvg,
				upperRankPct,
				percentileRank,
				testId,
				studentId);
	}
}
