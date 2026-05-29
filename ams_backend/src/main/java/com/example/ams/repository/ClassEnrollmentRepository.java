package com.example.ams.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.example.ams.domain.clazz.ClassEnrollment;

@Repository
public class ClassEnrollmentRepository {

	private static final RowMapper<ClassEnrollment> ROW_MAPPER = (rs, rowNum) -> new ClassEnrollment(
			rs.getLong("enrollment_id"),
			rs.getLong("class_id"),
			rs.getLong("student_id"),
			rs.getTimestamp("assigned_at").toInstant(),
			rs.getLong("assigned_by"));

	private final JdbcTemplate jdbcTemplate;

	public ClassEnrollmentRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Optional<ClassEnrollment> findById(long enrollmentId) {
		return queryOne("SELECT * FROM class_enrollment WHERE enrollment_id = ?", enrollmentId);
	}

	public boolean existsByClassIdAndStudentId(long classId, long studentId) {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM class_enrollment WHERE class_id = ? AND student_id = ?",
				Integer.class,
				classId,
				studentId);
		return count != null && count > 0;
	}

	public List<ClassEnrollment> findByClassId(long classId) {
		return jdbcTemplate.query(
				"SELECT * FROM class_enrollment WHERE class_id = ? ORDER BY assigned_at DESC",
				ROW_MAPPER,
				classId);
	}

	public ClassEnrollment insert(long classId, long studentId, long assignedBy) {
		String sql = """
				INSERT INTO class_enrollment (class_id, student_id, assigned_by)
				VALUES (?, ?, ?)
				""";
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(connection -> {
			var ps = connection.prepareStatement(sql, new String[] { "enrollment_id" });
			ps.setLong(1, classId);
			ps.setLong(2, studentId);
			ps.setLong(3, assignedBy);
			return ps;
		}, keyHolder);
		return findById(keyHolder.getKey().longValue()).orElseThrow();
	}

	public void deleteById(long enrollmentId) {
		jdbcTemplate.update("DELETE FROM class_enrollment WHERE enrollment_id = ?", enrollmentId);
	}

	private Optional<ClassEnrollment> queryOne(String sql, Object... args) {
		return jdbcTemplate.query(sql, ROW_MAPPER, args).stream().findFirst();
	}
}
