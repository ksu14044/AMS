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
			rs.getTimestamp("created_at").toInstant(),
			rs.getObject("question_count") != null ? rs.getInt("question_count") : null,
			rs.getObject("retake_threshold_count") != null ? rs.getInt("retake_threshold_count") : null,
			rs.getString("answer_key_pdf_path"),
			rs.getObject("parent_test_id") != null ? rs.getLong("parent_test_id") : null,
			rs.getInt("retake_attempt_no"));

	private final JdbcTemplate jdbcTemplate;

	public TestExamRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Optional<TestExam> findById(long testId) {
		return jdbcTemplate.query("SELECT * FROM test WHERE test_id = ?", ROW_MAPPER, testId).stream().findFirst();
	}

	public List<TestExam> findByClassId(long classId) {
		return jdbcTemplate.query(
				"SELECT * FROM test WHERE class_id = ? ORDER BY test_at DESC, test_id DESC",
				ROW_MAPPER,
				classId);
	}

	public List<TestExam> findRetakesByParentTestId(long parentTestId) {
		return jdbcTemplate.query(
				"SELECT * FROM test WHERE parent_test_id = ? ORDER BY retake_attempt_no",
				ROW_MAPPER,
				parentTestId);
	}

	public int countRetakes(long rootTestId) {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM test WHERE parent_test_id = ?",
				Integer.class,
				rootTestId);
		return count != null ? count : 0;
	}

	public TestExam insert(
			long classId,
			Long lessonRecordId,
			String title,
			Instant testAt,
			AssignmentStatus status,
			Integer questionCount,
			Integer retakeThresholdCount,
			Long parentTestId,
			int retakeAttemptNo) {
		String sql = """
				INSERT INTO test (
				    class_id, lesson_record_id, title, test_at, status,
				    question_count, retake_threshold_count, parent_test_id, retake_attempt_no
				) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
				""";
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(connection -> {
			var ps = connection.prepareStatement(sql, new String[] { "test_id" });
			ps.setLong(1, classId);
			if (lessonRecordId != null) {
				ps.setLong(2, lessonRecordId);
			} else {
				ps.setNull(2, java.sql.Types.BIGINT);
			}
			ps.setString(3, title);
			ps.setTimestamp(4, java.sql.Timestamp.from(testAt));
			ps.setString(5, status.name());
			if (questionCount != null) {
				ps.setInt(6, questionCount);
			} else {
				ps.setNull(6, java.sql.Types.INTEGER);
			}
			if (retakeThresholdCount != null) {
				ps.setInt(7, retakeThresholdCount);
			} else {
				ps.setNull(7, java.sql.Types.INTEGER);
			}
			if (parentTestId != null) {
				ps.setLong(8, parentTestId);
			} else {
				ps.setNull(8, java.sql.Types.BIGINT);
			}
			ps.setInt(9, retakeAttemptNo);
			return ps;
		}, keyHolder);
		return findById(keyHolder.getKey().longValue()).orElseThrow();
	}

	public List<TestSummary> findSummariesByLessonRecordId(long lessonRecordId) {
		return jdbcTemplate.query(
				"""
						SELECT test_id, title, question_count, retake_threshold_count, status, retake_attempt_no
						FROM test WHERE lesson_record_id = ? ORDER BY test_id
						""",
				(rs, rowNum) -> new TestSummary(
						rs.getLong("test_id"),
						rs.getString("title"),
						rs.getObject("question_count") != null ? rs.getInt("question_count") : null,
						rs.getObject("retake_threshold_count") != null ? rs.getInt("retake_threshold_count") : null,
						AssignmentStatus.valueOf(rs.getString("status")),
						rs.getInt("retake_attempt_no")),
				lessonRecordId);
	}

	public record TestSummary(
			long testId,
			String title,
			Integer questionCount,
			Integer retakeThresholdCount,
			AssignmentStatus status,
			int retakeAttemptNo) {
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

	public void updateQuestionCount(long testId, int questionCount) {
		jdbcTemplate.update("UPDATE test SET question_count = ? WHERE test_id = ?", questionCount, testId);
	}

	public void updateAnswerKeyPdfPath(long testId, String path) {
		jdbcTemplate.update("UPDATE test SET answer_key_pdf_path = ? WHERE test_id = ?", path, testId);
	}

	public void updateMetadata(long testId, String title, Integer questionCount, Integer retakeThresholdCount) {
		jdbcTemplate.update(
				"""
						UPDATE test
						SET title = ?, question_count = ?, retake_threshold_count = ?
						WHERE test_id = ?
						""",
				title,
				questionCount,
				retakeThresholdCount,
				testId);
	}

	public Long findLessonRecordId(long testId) {
		return jdbcTemplate.query(
				"SELECT lesson_record_id FROM test WHERE test_id = ?",
				rs -> rs.next() ? rs.getLong("lesson_record_id") : null,
				testId);
	}

	public void deleteById(long testId) {
		jdbcTemplate.update("DELETE FROM test WHERE test_id = ?", testId);
	}

	public void updateClassAverage(long testId, BigDecimal classAverage) {
		jdbcTemplate.update("UPDATE test SET class_average = ? WHERE test_id = ?", classAverage, testId);
	}

	public Optional<TestExam> findLatestCompletedRootInPeriod(long classId, Instant periodStart, Instant periodEnd) {
		return jdbcTemplate.query(
				"""
						SELECT * FROM test
						WHERE class_id = ? AND status = 'COMPLETED'
						  AND parent_test_id IS NULL
						  AND test_at >= ? AND test_at <= ?
						ORDER BY test_at DESC, test_id DESC
						LIMIT 1
						""",
				ROW_MAPPER,
				classId,
				java.sql.Timestamp.from(periodStart),
				java.sql.Timestamp.from(periodEnd))
				.stream()
				.findFirst();
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
