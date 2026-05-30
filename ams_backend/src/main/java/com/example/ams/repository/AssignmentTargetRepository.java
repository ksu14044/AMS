package com.example.ams.repository;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.ams.domain.clazz.AssignmentEntityType;

@Repository
public class AssignmentTargetRepository {

	private final JdbcTemplate jdbcTemplate;

	public AssignmentTargetRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<Long> findStudentIdsByEntity(AssignmentEntityType entityType, long entityId) {
		return jdbcTemplate.query(
				"""
						SELECT student_id
						FROM assignment_target
						WHERE entity_type = ? AND entity_id = ?
						ORDER BY student_id
						""",
				(rs, rowNum) -> rs.getLong("student_id"),
				entityType.name(),
				entityId);
	}

	public boolean hasExplicitTargets(AssignmentEntityType entityType, long entityId) {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM assignment_target WHERE entity_type = ? AND entity_id = ?",
				Integer.class,
				entityType.name(),
				entityId);
		return count != null && count > 0;
	}

	public void replaceAll(AssignmentEntityType entityType, long entityId, List<Long> studentIds) {
		jdbcTemplate.update(
				"DELETE FROM assignment_target WHERE entity_type = ? AND entity_id = ?",
				entityType.name(),
				entityId);
		for (long studentId : studentIds) {
			jdbcTemplate.update(
					"""
							INSERT INTO assignment_target (entity_type, entity_id, student_id)
							VALUES (?, ?, ?)
							""",
					entityType.name(),
					entityId,
					studentId);
		}
	}

	public void deleteByEntity(AssignmentEntityType entityType, long entityId) {
		jdbcTemplate.update(
				"DELETE FROM assignment_target WHERE entity_type = ? AND entity_id = ?",
				entityType.name(),
				entityId);
	}
}
