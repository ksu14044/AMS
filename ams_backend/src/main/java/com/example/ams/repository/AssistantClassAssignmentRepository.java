package com.example.ams.repository;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class AssistantClassAssignmentRepository {

	public record AssignedAssistant(long assignmentId, long assistantId, String assistantName) {
	}

	private static final RowMapper<AssignedAssistant> ASSIGNED_MAPPER = (rs, rowNum) -> new AssignedAssistant(
			rs.getLong("assignment_id"),
			rs.getLong("assistant_id"),
			rs.getString("assistant_name"));

	private final JdbcTemplate jdbcTemplate;

	public AssistantClassAssignmentRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public boolean existsByClassIdAndAssistantId(long classId, long assistantId) {
		Integer count = jdbcTemplate.queryForObject(
				"""
						SELECT COUNT(*) FROM assistant_class_assignment
						WHERE class_id = ? AND assistant_id = ?
						""",
				Integer.class,
				classId,
				assistantId);
		return count != null && count > 0;
	}

	public List<AssignedAssistant> findAssignedByClassId(long classId) {
		return jdbcTemplate.query(
				"""
						SELECT a.assignment_id, a.assistant_id, u.name AS assistant_name
						FROM assistant_class_assignment a
						INNER JOIN `user` u ON a.assistant_id = u.user_id
						WHERE a.class_id = ?
						ORDER BY u.name ASC
						""",
				ASSIGNED_MAPPER,
				classId);
	}

	public void deleteByClassId(long classId) {
		jdbcTemplate.update("DELETE FROM assistant_class_assignment WHERE class_id = ?", classId);
	}

	public void insert(long classId, long assistantId, long assignedBy) {
		jdbcTemplate.update(
				"""
						INSERT INTO assistant_class_assignment (assistant_id, class_id, assigned_by)
						VALUES (?, ?, ?)
						""",
				assistantId,
				classId,
				assignedBy);
	}
}
