package com.example.ams.repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.example.ams.domain.user.StudentRosterRow;
import com.example.ams.domain.user.UserStatus;

@Repository
public class StudentRosterRepository {

	private static final RowMapper<StudentBaseRow> BASE_ROW_MAPPER = (rs, rowNum) -> new StudentBaseRow(
			rs.getLong("user_id"),
			rs.getString("name"),
			rs.getString("email"),
			rs.getString("phone_number"),
			UserStatus.valueOf(rs.getString("status")),
			rs.getTimestamp("created_at").toInstant());

	private final JdbcTemplate jdbcTemplate;

	public StudentRosterRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<StudentRosterRow> findForAcademy(long academyId, String search) {
		String sql = """
				SELECT DISTINCT u.user_id, u.name, u.email, u.phone_number, u.status, u.created_at
				FROM `user` u
				WHERE u.academy_id = ? AND u.role = 'STUDENT'
				"""
				+ searchClause("u", search)
				+ """
				 ORDER BY u.created_at DESC, u.name ASC
				""";
		return loadWithClasses(sql, searchParams(academyId, search));
	}

	public List<StudentRosterRow> findForHomeroomTeacher(long academyId, long teacherId, String search) {
		String sql = """
				SELECT DISTINCT u.user_id, u.name, u.email, u.phone_number, u.status, u.created_at
				FROM `user` u
				INNER JOIN class_enrollment e ON e.student_id = u.user_id
				INNER JOIN `class` c ON c.class_id = e.class_id AND c.homeroom_teacher_id = ?
				WHERE u.academy_id = ? AND u.role = 'STUDENT'
				"""
				+ searchClause("u", search)
				+ """
				 ORDER BY u.created_at DESC, u.name ASC
				""";
		List<Object> params = new ArrayList<>();
		params.add(teacherId);
		params.add(academyId);
		addSearchParams(params, search);
		return loadWithClasses(sql, params.toArray());
	}

	private List<StudentRosterRow> loadWithClasses(String studentSql, Object... studentArgs) {
		List<StudentBaseRow> bases = jdbcTemplate.query(studentSql, BASE_ROW_MAPPER, studentArgs);
		if (bases.isEmpty()) {
			return List.of();
		}
		List<Long> ids = bases.stream().map(StudentBaseRow::userId).toList();
		String in = String.join(",", ids.stream().map(id -> "?").toList());
		String classSql = """
				SELECT e.student_id, c.class_id, c.name, c.subject
				FROM class_enrollment e
				INNER JOIN `class` c ON c.class_id = e.class_id
				WHERE e.student_id IN (%s)
				ORDER BY c.name ASC
				""".formatted(in);
		Map<Long, List<StudentRosterRow.EnrolledClass>> classesByStudent = new LinkedHashMap<>();
		for (Long id : ids) {
			classesByStudent.put(id, new ArrayList<>());
		}
		jdbcTemplate.query(
				classSql,
				(rs, rowNum) -> {
					long studentId = rs.getLong("student_id");
					classesByStudent
							.computeIfAbsent(studentId, ignored -> new ArrayList<>())
							.add(new StudentRosterRow.EnrolledClass(
									rs.getLong("class_id"),
									rs.getString("name"),
									rs.getString("subject")));
					return null;
				},
				ids.toArray());
		return bases.stream()
				.map(b -> new StudentRosterRow(
						b.userId(),
						b.name(),
						b.email(),
						b.phoneNumber(),
						b.status(),
						b.createdAt(),
						List.copyOf(classesByStudent.getOrDefault(b.userId(), List.of()))))
				.toList();
	}

	private static String searchClause(String userAlias, String search) {
		if (!StringUtils.hasText(search)) {
			return "";
		}
		return """
				 AND (
				   %s.name LIKE ? OR %s.email LIKE ? OR %s.phone_number LIKE ?
				   OR EXISTS (
				     SELECT 1 FROM class_enrollment se
				     INNER JOIN `class` sc ON sc.class_id = se.class_id
				     WHERE se.student_id = %s.user_id AND sc.name LIKE ?
				   )
				 )
				"""
				.formatted(userAlias, userAlias, userAlias, userAlias);
	}

	private static Object[] searchParams(long academyId, String search) {
		List<Object> params = new ArrayList<>();
		params.add(academyId);
		addSearchParams(params, search);
		return params.toArray();
	}

	private static void addSearchParams(List<Object> params, String search) {
		if (!StringUtils.hasText(search)) {
			return;
		}
		String pattern = "%" + search.trim() + "%";
		params.add(pattern);
		params.add(pattern);
		params.add(pattern);
		params.add(pattern);
	}

	private record StudentBaseRow(
			long userId,
			String name,
			String email,
			String phoneNumber,
			UserStatus status,
			java.time.Instant createdAt) {
	}
}
