package com.example.ams.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.example.ams.domain.clazz.LessonRecord;

@Repository
public class LessonRecordRepository {

	private static final RowMapper<LessonRecord> ROW_MAPPER = (rs, rowNum) -> new LessonRecord(
			rs.getLong("lesson_record_id"),
			rs.getLong("class_id"),
			rs.getDate("lesson_date").toLocalDate(),
			rs.getString("summary"),
			rs.getLong("author_id"),
			rs.getTimestamp("created_at").toInstant(),
			rs.getTimestamp("updated_at").toInstant());

	private final JdbcTemplate jdbcTemplate;

	public LessonRecordRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Optional<LessonRecord> findById(long lessonRecordId) {
		return jdbcTemplate
				.query("SELECT * FROM lesson_record WHERE lesson_record_id = ?", ROW_MAPPER, lessonRecordId)
				.stream()
				.findFirst();
	}

	public List<LessonRecord> findByClassId(long classId) {
		return jdbcTemplate.query(
				"SELECT * FROM lesson_record WHERE class_id = ? ORDER BY lesson_date DESC",
				ROW_MAPPER,
				classId);
	}

	public boolean existsByClassIdAndLessonDate(long classId, LocalDate lessonDate) {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM lesson_record WHERE class_id = ? AND lesson_date = ?",
				Integer.class,
				classId,
				java.sql.Date.valueOf(lessonDate));
		return count != null && count > 0;
	}

	public LessonRecord insert(long classId, LocalDate lessonDate, String summary, long authorId) {
		String sql = """
				INSERT INTO lesson_record (class_id, lesson_date, summary, author_id)
				VALUES (?, ?, ?, ?)
				""";
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(connection -> {
			var ps = connection.prepareStatement(sql, new String[] { "lesson_record_id" });
			ps.setLong(1, classId);
			ps.setDate(2, java.sql.Date.valueOf(lessonDate));
			ps.setString(3, summary);
			ps.setLong(4, authorId);
			return ps;
		}, keyHolder);
		return findById(keyHolder.getKey().longValue()).orElseThrow();
	}

	public void updateSummary(long lessonRecordId, String summary) {
		jdbcTemplate.update(
				"UPDATE lesson_record SET summary = ? WHERE lesson_record_id = ?",
				summary,
				lessonRecordId);
	}

	public int countHomework(long lessonRecordId) {
		return countLinked("homework", lessonRecordId);
	}

	public int countTests(long lessonRecordId) {
		return countLinked("test", lessonRecordId);
	}

	public int countVideos(long lessonRecordId) {
		return countLinked("video_lesson", lessonRecordId);
	}

	public int countClinicSlots(long lessonRecordId) {
		return countLinked("clinic_slot", lessonRecordId);
	}

	private int countLinked(String table, long lessonRecordId) {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM " + table + " WHERE lesson_record_id = ?",
				Integer.class,
				lessonRecordId);
		return count != null ? count : 0;
	}
}
