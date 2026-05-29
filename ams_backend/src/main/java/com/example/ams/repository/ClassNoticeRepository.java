package com.example.ams.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.example.ams.domain.clazz.ClassNotice;

@Repository
public class ClassNoticeRepository {

	private static final RowMapper<ClassNotice> ROW_MAPPER = (rs, rowNum) -> new ClassNotice(
			rs.getLong("notice_id"),
			rs.getLong("class_id"),
			rs.getString("title"),
			rs.getString("body"),
			rs.getString("attachment_url"),
			rs.getTimestamp("published_at").toInstant(),
			rs.getTimestamp("scheduled_at") != null ? rs.getTimestamp("scheduled_at").toInstant() : null,
			rs.getLong("author_id"),
			rs.getTimestamp("created_at").toInstant(),
			rs.getTimestamp("updated_at").toInstant());

	private final JdbcTemplate jdbcTemplate;

	public ClassNoticeRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<ClassNotice> findByClassId(long classId) {
		return jdbcTemplate.query(
				"""
						SELECT * FROM class_notice
						WHERE class_id = ?
						ORDER BY published_at DESC
						""",
				ROW_MAPPER,
				classId);
	}

	public Optional<ClassNotice> findById(long noticeId) {
		return jdbcTemplate.query(
				"SELECT * FROM class_notice WHERE notice_id = ?",
				ROW_MAPPER,
				noticeId).stream().findFirst();
	}

	public ClassNotice insert(long classId, String title, String body, String attachmentUrl, long authorId) {
		String sql = """
				INSERT INTO class_notice (class_id, title, body, attachment_url, author_id)
				VALUES (?, ?, ?, ?, ?)
				""";
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(connection -> {
			var ps = connection.prepareStatement(sql, new String[] { "notice_id" });
			ps.setLong(1, classId);
			ps.setString(2, title);
			ps.setString(3, body);
			ps.setString(4, attachmentUrl);
			ps.setLong(5, authorId);
			return ps;
		}, keyHolder);
		return findById(keyHolder.getKey().longValue()).orElseThrow();
	}
}
