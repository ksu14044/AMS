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

import com.example.ams.domain.clazz.AssignmentStatus;
import com.example.ams.domain.clazz.TestExam;

@Repository
public class TestExamRepository {

	private static final RowMapper<TestExam> ROW_MAPPER = (rs, rowNum) -> new TestExam(
			rs.getLong("test_id"),
			rs.getLong("class_id"),
			rs.getString("title"),
			rs.getTimestamp("test_at").toInstant(),
			AssignmentStatus.valueOf(rs.getString("status")),
			rs.getBigDecimal("class_average"),
			rs.getTimestamp("completed_at") != null ? rs.getTimestamp("completed_at").toInstant() : null,
			rs.getTimestamp("created_at").toInstant());

	private final JdbcTemplate jdbcTemplate;

	public TestExamRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Optional<TestExam> findById(long testId) {
		return jdbcTemplate.query("SELECT * FROM test WHERE test_id = ?", ROW_MAPPER, testId).stream().findFirst();
	}

	public List<TestExam> findByClassId(long classId) {
		return jdbcTemplate.query(
				"SELECT * FROM test WHERE class_id = ? ORDER BY test_at DESC",
				ROW_MAPPER,
				classId);
	}

	public TestExam insert(long classId, String title, Instant testAt, AssignmentStatus status) {
		String sql = "INSERT INTO test (class_id, title, test_at, status) VALUES (?, ?, ?, ?)";
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(connection -> {
			var ps = connection.prepareStatement(sql, new String[] { "test_id" });
			ps.setLong(1, classId);
			ps.setString(2, title);
			ps.setTimestamp(3, java.sql.Timestamp.from(testAt));
			ps.setString(4, status.name());
			return ps;
		}, keyHolder);
		return findById(keyHolder.getKey().longValue()).orElseThrow();
	}

	public void complete(long testId, BigDecimal classAverage, Instant completedAt) {
		jdbcTemplate.update(
				"""
						UPDATE test
						SET status = 'COMPLETED', class_average = ?, completed_at = ?
						WHERE test_id = ?
						""",
				classAverage,
				java.sql.Timestamp.from(completedAt),
				testId);
	}

	public Optional<TestExam> findPreviousCompleted(long classId, Instant beforeTestAt) {
		return jdbcTemplate.query(
				"""
						SELECT * FROM test
						WHERE class_id = ? AND status = 'COMPLETED' AND test_at < ?
						ORDER BY test_at DESC
						LIMIT 1
						""",
				ROW_MAPPER,
				classId,
				java.sql.Timestamp.from(beforeTestAt))
				.stream()
				.findFirst();
	}

	public List<TestExam> findScheduledTestBetween(Instant startInclusive, Instant endExclusive) {
		return jdbcTemplate.query(
				"""
						SELECT * FROM test
						WHERE status = 'SCHEDULED' AND test_at >= ? AND test_at < ?
						ORDER BY test_at ASC
						""",
				ROW_MAPPER,
				java.sql.Timestamp.from(startInclusive),
				java.sql.Timestamp.from(endExclusive));
	}
}
