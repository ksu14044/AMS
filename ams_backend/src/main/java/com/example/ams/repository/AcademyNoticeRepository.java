package com.example.ams.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.example.ams.domain.academy.AcademyNotice;

@Repository
public class AcademyNoticeRepository {

	private static final RowMapper<AcademyNotice> ROW_MAPPER = (rs, rowNum) -> new AcademyNotice(
			rs.getLong("notice_id"),
			rs.getLong("academy_id"),
			rs.getString("title"),
			rs.getString("body"),
			rs.getString("attachment_url"),
			rs.getTimestamp("published_at").toInstant(),
			rs.getLong("author_id"),
			rs.getTimestamp("created_at").toInstant(),
			rs.getTimestamp("updated_at").toInstant());

	private final JdbcTemplate jdbcTemplate;

	public AcademyNoticeRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<AcademyNotice> findByAcademyId(long academyId) {
		return jdbcTemplate.query(
				"""
						SELECT * FROM academy_notice
						WHERE academy_id = ?
						ORDER BY published_at DESC
						""",
				ROW_MAPPER,
				academyId);
	}

	public Optional<AcademyNotice> findById(long noticeId) {
		return jdbcTemplate.query(
				"SELECT * FROM academy_notice WHERE notice_id = ?",
				ROW_MAPPER,
				noticeId).stream().findFirst();
	}

	public AcademyNotice insert(long academyId, String title, String body, String attachmentUrl, long authorId) {
		String sql = """
				INSERT INTO academy_notice (academy_id, title, body, attachment_url, author_id)
				VALUES (?, ?, ?, ?, ?)
				""";
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(connection -> {
			var ps = connection.prepareStatement(sql, new String[] { "notice_id" });
			ps.setLong(1, academyId);
			ps.setString(2, title);
			ps.setString(3, body);
			ps.setString(4, attachmentUrl);
			ps.setLong(5, authorId);
			return ps;
		}, keyHolder);
		return findById(keyHolder.getKey().longValue()).orElseThrow();
	}
}
