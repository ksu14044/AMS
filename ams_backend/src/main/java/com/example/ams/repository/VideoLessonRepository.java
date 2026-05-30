package com.example.ams.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.example.ams.domain.clazz.VideoLesson;

@Repository
public class VideoLessonRepository {

	private static final RowMapper<VideoLesson> ROW_MAPPER = (rs, rowNum) -> new VideoLesson(
			rs.getLong("video_id"),
			rs.getLong("class_id"),
			rs.getString("youtube_url"),
			rs.getString("title"),
			rs.getString("description"),
			rs.getString("thumbnail_url"),
			rs.getTimestamp("published_at").toInstant(),
			rs.getLong("author_id"),
			rs.getTimestamp("created_at").toInstant(),
			rs.getTimestamp("updated_at").toInstant());

	private final JdbcTemplate jdbcTemplate;

	public VideoLessonRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<VideoLesson> findByClassId(long classId) {
		return jdbcTemplate.query(
				"""
						SELECT * FROM video_lesson
						WHERE class_id = ?
						ORDER BY published_at DESC
						""",
				ROW_MAPPER,
				classId);
	}

	public Optional<VideoLesson> findById(long videoId) {
		return jdbcTemplate.query(
				"SELECT * FROM video_lesson WHERE video_id = ?",
				ROW_MAPPER,
				videoId).stream().findFirst();
	}

	public Optional<VideoLesson> findByIdAndClassId(long videoId, long classId) {
		return jdbcTemplate.query(
				"SELECT * FROM video_lesson WHERE video_id = ? AND class_id = ?",
				ROW_MAPPER,
				videoId,
				classId).stream().findFirst();
	}

	public VideoLesson insert(
			long classId,
			String youtubeUrl,
			String title,
			String description,
			String thumbnailUrl,
			Instant publishedAt,
			long authorId) {
		return insert(classId, null, youtubeUrl, title, description, thumbnailUrl, publishedAt, authorId);
	}

	public VideoLesson insert(
			long classId,
			Long lessonRecordId,
			String youtubeUrl,
			String title,
			String description,
			String thumbnailUrl,
			Instant publishedAt,
			long authorId) {
		String sql = """
				INSERT INTO video_lesson (class_id, lesson_record_id, youtube_url, title, description, thumbnail_url, published_at, author_id)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?)
				""";
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(connection -> {
			var ps = connection.prepareStatement(sql, new String[] { "video_id" });
			ps.setLong(1, classId);
			if (lessonRecordId != null) {
				ps.setLong(2, lessonRecordId);
			} else {
				ps.setNull(2, java.sql.Types.BIGINT);
			}
			ps.setString(3, youtubeUrl);
			ps.setString(4, title);
			ps.setString(5, description);
			ps.setString(6, thumbnailUrl);
			ps.setTimestamp(7, java.sql.Timestamp.from(publishedAt));
			ps.setLong(8, authorId);
			return ps;
		}, keyHolder);
		return findById(keyHolder.getKey().longValue()).orElseThrow();
	}

	public List<VideoSummary> findSummariesByLessonRecordId(long lessonRecordId) {
		return jdbcTemplate.query(
				"SELECT video_id, title FROM video_lesson WHERE lesson_record_id = ? ORDER BY video_id",
				(rs, rowNum) -> new VideoSummary(rs.getLong("video_id"), rs.getString("title")),
				lessonRecordId);
	}

	public record VideoSummary(long videoId, String title) {
	}

	public VideoLesson update(
			long videoId,
			long classId,
			String youtubeUrl,
			String title,
			String description,
			String thumbnailUrl) {
		jdbcTemplate.update(
				"""
						UPDATE video_lesson
						SET youtube_url = ?, title = ?, description = ?, thumbnail_url = ?
						WHERE video_id = ? AND class_id = ?
						""",
				youtubeUrl,
				title,
				description,
				thumbnailUrl,
				videoId,
				classId);
		return findByIdAndClassId(videoId, classId).orElseThrow();
	}

	public void delete(long videoId, long classId) {
		jdbcTemplate.update("DELETE FROM video_lesson WHERE video_id = ? AND class_id = ?", videoId, classId);
	}
}
