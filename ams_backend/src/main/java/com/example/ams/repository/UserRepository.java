package com.example.ams.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.example.ams.domain.user.Subject;
import com.example.ams.domain.user.User;
import com.example.ams.domain.user.UserRole;
import com.example.ams.domain.user.UserStatus;

@Repository
public class UserRepository {

	private static final RowMapper<User> ROW_MAPPER = (rs, rowNum) -> new User(
			rs.getLong("user_id"),
			rs.getLong("academy_id"),
			rs.getString("email"),
			rs.getString("password_hash"),
			rs.getString("name"),
			rs.getString("phone_number"),
			UserRole.valueOf(rs.getString("role")),
			rs.getString("subject") != null ? Subject.valueOf(rs.getString("subject")) : null,
			UserStatus.valueOf(rs.getString("status")),
			rs.getTimestamp("personal_info_consent_at").toInstant(),
			rs.getTimestamp("created_at").toInstant(),
			rs.getTimestamp("updated_at").toInstant());

	private final JdbcTemplate jdbcTemplate;

	public UserRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Optional<User> findById(long userId) {
		return queryOne("SELECT * FROM `user` WHERE user_id = ?", userId);
	}

	public Optional<User> findByAcademyIdAndEmail(long academyId, String email) {
		return queryOne("SELECT * FROM `user` WHERE academy_id = ? AND email = ?", academyId, email);
	}

	public List<User> findAllByEmail(String email) {
		return jdbcTemplate.query("SELECT * FROM `user` WHERE email = ?", ROW_MAPPER, email);
	}

	public boolean existsByAcademyIdAndEmail(long academyId, String email) {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM `user` WHERE academy_id = ? AND email = ?",
				Integer.class,
				academyId,
				email);
		return count != null && count > 0;
	}

	public User insert(
			long academyId,
			String email,
			String passwordHash,
			String name,
			UserRole role,
			Subject subject,
			UserStatus status,
			String phoneNumber,
			java.time.Instant personalInfoConsentAt) {
		String sql = """
				INSERT INTO `user` (
					academy_id, email, password_hash, name, role, subject, status, phone_number, personal_info_consent_at
				)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
				""";
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(connection -> {
			var ps = connection.prepareStatement(sql, new String[] { "user_id" });
			ps.setLong(1, academyId);
			ps.setString(2, email);
			ps.setString(3, passwordHash);
			ps.setString(4, name);
			ps.setString(5, role.name());
			ps.setString(6, subject != null ? subject.name() : null);
			ps.setString(7, status.name());
			ps.setString(8, phoneNumber);
			ps.setTimestamp(9, java.sql.Timestamp.from(personalInfoConsentAt));
			return ps;
		}, keyHolder);
		return findById(keyHolder.getKey().longValue()).orElseThrow();
	}

	public List<User> findActiveByAcademyIdAndRole(long academyId, UserRole role) {
		String sql = """
				SELECT * FROM `user`
				WHERE academy_id = ? AND `role` = ? AND `status` = 'ACTIVE'
				ORDER BY name ASC
				""";
		return jdbcTemplate.query(sql, ROW_MAPPER, academyId, role.name());
	}

	public List<User> findActiveStudentsByAcademyId(long academyId) {
		return findActiveByAcademyIdAndRole(academyId, UserRole.STUDENT);
	}

	public List<User> findActiveAssistantsByAcademyId(long academyId) {
		String sql = """
				SELECT * FROM `user`
				WHERE academy_id = ? AND `status` = 'ACTIVE'
				  AND `role` IN ('ASSISTANT_KO', 'ASSISTANT_EN', 'ASSISTANT_MATH')
				ORDER BY name ASC
				""";
		return jdbcTemplate.query(sql, ROW_MAPPER, academyId);
	}

	public List<User> findActiveHomeroomTeachersByAcademyId(long academyId) {
		String sql = """
				SELECT * FROM `user`
				WHERE academy_id = ? AND `status` = 'ACTIVE'
				  AND `role` IN ('TEACHER_KO', 'TEACHER_EN', 'TEACHER_MATH')
				ORDER BY name ASC
				""";
		return jdbcTemplate.query(sql, ROW_MAPPER, academyId);
	}

	public List<User> findPendingStaffByAcademyId(long academyId) {
		String sql = """
				SELECT * FROM `user`
				WHERE academy_id = ? AND `status` = 'PENDING'
				  AND `role` NOT IN ('STUDENT', 'ACADEMY_ADMIN')
				ORDER BY created_at ASC
				""";
		return jdbcTemplate.query(sql, ROW_MAPPER, academyId);
	}

	public User updateRoleAndStatus(long userId, UserRole role, Subject subject, UserStatus status) {
		jdbcTemplate.update(
				"UPDATE `user` SET role = ?, subject = ?, status = ? WHERE user_id = ?",
				role.name(),
				subject != null ? subject.name() : null,
				status.name(),
				userId);
		return findById(userId).orElseThrow();
	}

	private Optional<User> queryOne(String sql, Object... args) {
		return jdbcTemplate.query(sql, ROW_MAPPER, args).stream().findFirst();
	}
}
