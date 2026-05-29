package com.example.ams.repository;

import java.time.Instant;
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
			rs.getTimestamp("due_at").toInstant(),
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
				"SELECT * FROM homework WHERE class_id = ? ORDER BY due_at DESC",
				ROW_MAPPER,
				classId);
	}

	public Homework insert(long classId, String title, Instant dueAt, AssignmentStatus status) {
		String sql = "INSERT INTO homework (class_id, title, due_at, status) VALUES (?, ?, ?, ?)";
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(connection -> {
			var ps = connection.prepareStatement(sql, new String[] { "homework_id" });
			ps.setLong(1, classId);
			ps.setString(2, title);
			ps.setTimestamp(3, java.sql.Timestamp.from(dueAt));
			ps.setString(4, status.name());
			return ps;
		}, keyHolder);
		return findById(keyHolder.getKey().longValue()).orElseThrow();
	}

	public void updateStatus(long homeworkId, AssignmentStatus status) {
		jdbcTemplate.update("UPDATE homework SET status = ? WHERE homework_id = ?", status.name(), homeworkId);
	}

	public List<Homework> findScheduledDueBetween(java.time.Instant startInclusive, java.time.Instant endExclusive) {
		return jdbcTemplate.query(
				"""
						SELECT * FROM homework
						WHERE status = 'SCHEDULED' AND due_at >= ? AND due_at < ?
						ORDER BY due_at ASC
						""",
				ROW_MAPPER,
				java.sql.Timestamp.from(startInclusive),
				java.sql.Timestamp.from(endExclusive));
	}
}
