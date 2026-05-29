package com.example.ams.repository;

import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.example.ams.domain.clazz.Textbook;

@Repository
public class TextbookRepository {

	private static final RowMapper<Textbook> ROW_MAPPER = (rs, rowNum) -> new Textbook(
			rs.getLong("class_id"),
			rs.getString("title"),
			rs.getString("publisher"),
			rs.getString("progress_note"),
			rs.getTimestamp("updated_at").toInstant());

	private final JdbcTemplate jdbcTemplate;

	public TextbookRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Optional<Textbook> findByClassId(long classId) {
		return jdbcTemplate.query(
				"SELECT * FROM textbook WHERE class_id = ?",
				ROW_MAPPER,
				classId).stream().findFirst();
	}

	public Textbook upsert(long classId, String title, String publisher, String progressNote) {
		jdbcTemplate.update(
				"""
						INSERT INTO textbook (class_id, title, publisher, progress_note)
						VALUES (?, ?, ?, ?)
						ON DUPLICATE KEY UPDATE
						  title = VALUES(title),
						  publisher = VALUES(publisher),
						  progress_note = VALUES(progress_note)
						""",
				classId,
				title,
				publisher,
				progressNote);
		return findByClassId(classId).orElseThrow();
	}
}
