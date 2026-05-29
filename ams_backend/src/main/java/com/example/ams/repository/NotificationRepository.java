package com.example.ams.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.example.ams.domain.notification.Notification;
import com.example.ams.domain.notification.NotificationReferenceType;
import com.example.ams.domain.notification.NotificationType;

@Repository
public class NotificationRepository {

	private static final RowMapper<Notification> ROW_MAPPER = (rs, rowNum) -> new Notification(
			rs.getLong("notification_id"),
			rs.getLong("academy_id"),
			rs.getLong("user_id"),
			NotificationType.valueOf(rs.getString("type")),
			rs.getString("title"),
			rs.getString("body"),
			rs.getString("reference_type") != null
					? NotificationReferenceType.valueOf(rs.getString("reference_type"))
					: null,
			rs.getObject("reference_id") != null ? rs.getLong("reference_id") : null,
			rs.getTimestamp("read_at") != null ? rs.getTimestamp("read_at").toInstant() : null,
			rs.getTimestamp("created_at").toInstant());

	private final JdbcTemplate jdbcTemplate;

	public NotificationRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Notification insert(
			long academyId,
			long userId,
			NotificationType type,
			String title,
			String body,
			NotificationReferenceType referenceType,
			Long referenceId) {
		String sql = """
				INSERT INTO notification (academy_id, user_id, type, title, body, reference_type, reference_id)
				VALUES (?, ?, ?, ?, ?, ?, ?)
				""";
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(connection -> {
			var ps = connection.prepareStatement(sql, new String[] { "notification_id" });
			ps.setLong(1, academyId);
			ps.setLong(2, userId);
			ps.setString(3, type.name());
			ps.setString(4, title);
			ps.setString(5, body);
			ps.setString(6, referenceType != null ? referenceType.name() : null);
			if (referenceId != null) {
				ps.setLong(7, referenceId);
			} else {
				ps.setNull(7, java.sql.Types.BIGINT);
			}
			return ps;
		}, keyHolder);
		return findById(keyHolder.getKey().longValue()).orElseThrow();
	}

	public Optional<Notification> findById(long notificationId) {
		return queryOne("SELECT * FROM notification WHERE notification_id = ?", notificationId);
	}

	public Optional<Notification> findByIdAndUserId(long notificationId, long userId) {
		return queryOne(
				"SELECT * FROM notification WHERE notification_id = ? AND user_id = ?",
				notificationId,
				userId);
	}

	public List<Notification> findByUserId(long userId, long academyId, boolean unreadOnly) {
		String sql = unreadOnly
				? """
						SELECT * FROM notification
						WHERE user_id = ? AND academy_id = ? AND read_at IS NULL
						ORDER BY created_at DESC
						"""
				: """
						SELECT * FROM notification
						WHERE user_id = ? AND academy_id = ?
						ORDER BY created_at DESC
						""";
		return jdbcTemplate.query(sql, ROW_MAPPER, userId, academyId);
	}

	public int countUnread(long userId, long academyId) {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM notification WHERE user_id = ? AND academy_id = ? AND read_at IS NULL",
				Integer.class,
				userId,
				academyId);
		return count != null ? count : 0;
	}

	public boolean markRead(long notificationId, long userId) {
		int updated = jdbcTemplate.update(
				"UPDATE notification SET read_at = CURRENT_TIMESTAMP WHERE notification_id = ? AND user_id = ? AND read_at IS NULL",
				notificationId,
				userId);
		return updated > 0;
	}

	public int markAllRead(long userId, long academyId) {
		return jdbcTemplate.update(
				"UPDATE notification SET read_at = CURRENT_TIMESTAMP WHERE user_id = ? AND academy_id = ? AND read_at IS NULL",
				userId,
				academyId);
	}

	public boolean existsByUserAndTypeAndReference(
			long userId,
			NotificationType type,
			NotificationReferenceType referenceType,
			long referenceId) {
		Integer count = jdbcTemplate.queryForObject(
				"""
						SELECT COUNT(*) FROM notification
						WHERE user_id = ? AND type = ? AND reference_type = ? AND reference_id = ?
						""",
				Integer.class,
				userId,
				type.name(),
				referenceType.name(),
				referenceId);
		return count != null && count > 0;
	}

	private Optional<Notification> queryOne(String sql, Object... args) {
		return jdbcTemplate.query(sql, ROW_MAPPER, args).stream().findFirst();
	}
}
