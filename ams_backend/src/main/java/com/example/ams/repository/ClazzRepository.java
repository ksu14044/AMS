package com.example.ams.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.example.ams.domain.clazz.Clazz;
import com.example.ams.domain.user.Subject;

@Repository
public class ClazzRepository {

	private static final RowMapper<Clazz> ROW_MAPPER = (rs, rowNum) -> new Clazz(
			rs.getLong("class_id"),
			rs.getLong("academy_id"),
			Subject.valueOf(rs.getString("subject")),
			rs.getString("name"),
			rs.getLong("homeroom_teacher_id"),
			rs.getString("classroom"),
			rs.getTimestamp("created_at").toInstant());

	private final JdbcTemplate jdbcTemplate;

	public ClazzRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Optional<Clazz> findById(long classId) {
		return queryOne("SELECT * FROM `class` WHERE class_id = ?", classId);
	}

	public List<Clazz> findByAcademyId(long academyId) {
		return jdbcTemplate.query(
				"SELECT * FROM `class` WHERE academy_id = ? ORDER BY created_at DESC",
				ROW_MAPPER,
				academyId);
	}

	public List<Clazz> findByHomeroomTeacherId(long teacherId) {
		return jdbcTemplate.query(
				"SELECT * FROM `class` WHERE homeroom_teacher_id = ? ORDER BY name ASC",
				ROW_MAPPER,
				teacherId);
	}

	public List<Clazz> findByAssistantId(long assistantId) {
		String sql = """
				SELECT c.* FROM `class` c
				INNER JOIN assistant_class_assignment a ON c.class_id = a.class_id
				WHERE a.assistant_id = ?
				ORDER BY c.name ASC
				""";
		return jdbcTemplate.query(sql, ROW_MAPPER, assistantId);
	}

	public List<Clazz> findByStudentId(long studentId) {
		String sql = """
				SELECT c.* FROM `class` c
				INNER JOIN class_enrollment e ON c.class_id = e.class_id
				WHERE e.student_id = ?
				ORDER BY c.name ASC
				""";
		return jdbcTemplate.query(sql, ROW_MAPPER, studentId);
	}

	public boolean existsByAcademyIdAndName(long academyId, String name) {
		return existsByAcademyIdAndNameExcluding(academyId, name, null);
	}

	public boolean existsByAcademyIdAndNameExcluding(long academyId, String name, Long excludeClassId) {
		String sql = excludeClassId == null
				? "SELECT COUNT(*) FROM `class` WHERE academy_id = ? AND name = ?"
				: "SELECT COUNT(*) FROM `class` WHERE academy_id = ? AND name = ? AND class_id <> ?";
		Integer count = excludeClassId == null
				? jdbcTemplate.queryForObject(sql, Integer.class, academyId, name)
				: jdbcTemplate.queryForObject(sql, Integer.class, academyId, name, excludeClassId);
		return count != null && count > 0;
	}

	public Clazz update(
			long classId,
			Subject subject,
			String name,
			long homeroomTeacherId,
			String classroom) {
		jdbcTemplate.update(
				"""
						UPDATE `class`
						SET subject = ?, name = ?, homeroom_teacher_id = ?, classroom = ?
						WHERE class_id = ?
						""",
				subject.name(),
				name,
				homeroomTeacherId,
				classroom,
				classId);
		return findById(classId).orElseThrow();
	}

	public Clazz insert(
			long academyId,
			Subject subject,
			String name,
			long homeroomTeacherId,
			String classroom) {
		String sql = """
				INSERT INTO `class` (academy_id, subject, name, homeroom_teacher_id, classroom)
				VALUES (?, ?, ?, ?, ?)
				""";
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(connection -> {
			var ps = connection.prepareStatement(sql, new String[] { "class_id" });
			ps.setLong(1, academyId);
			ps.setString(2, subject.name());
			ps.setString(3, name);
			ps.setLong(4, homeroomTeacherId);
			ps.setString(5, classroom);
			return ps;
		}, keyHolder);
		return findById(keyHolder.getKey().longValue()).orElseThrow();
	}

	private Optional<Clazz> queryOne(String sql, Object... args) {
		return jdbcTemplate.query(sql, ROW_MAPPER, args).stream().findFirst();
	}
}
