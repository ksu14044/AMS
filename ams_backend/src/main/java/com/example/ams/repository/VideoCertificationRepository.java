package com.example.ams.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.example.ams.domain.clazz.VideoCertification;

@Repository
public class VideoCertificationRepository {

	private static final RowMapper<VideoCertification> ROW_MAPPER = (rs, rowNum) -> new VideoCertification(
			rs.getLong("certification_id"),
			rs.getLong("video_id"),
			rs.getLong("student_id"),
			rs.getString("image_url"),
			rs.getTimestamp("submitted_at").toInstant());

	private final JdbcTemplate jdbcTemplate;

	public VideoCertificationRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<VideoCertification> findByVideoId(long videoId) {
		return jdbcTemplate.query(
				"""
						SELECT * FROM video_certification
						WHERE video_id = ?
						ORDER BY submitted_at DESC
						""",
				ROW_MAPPER,
				videoId);
	}

	public Optional<VideoCertification> findByVideoIdAndStudentId(long videoId, long studentId) {
		return jdbcTemplate.query(
				"""
						SELECT * FROM video_certification
						WHERE video_id = ? AND student_id = ?
						""",
				ROW_MAPPER,
				videoId,
				studentId).stream().findFirst();
	}

	public VideoCertification insert(long videoId, long studentId, String imageUrl) {
		String sql = """
				INSERT INTO video_certification (video_id, student_id, image_url)
				VALUES (?, ?, ?)
				""";
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(connection -> {
			var ps = connection.prepareStatement(sql, new String[] { "certification_id" });
			ps.setLong(1, videoId);
			ps.setLong(2, studentId);
			ps.setString(3, imageUrl);
			return ps;
		}, keyHolder);
		long id = keyHolder.getKey().longValue();
		return jdbcTemplate.query("SELECT * FROM video_certification WHERE certification_id = ?", ROW_MAPPER, id)
				.stream()
				.findFirst()
				.orElseThrow();
	}

	public void updateImage(long certificationId, String imageUrl) {
		jdbcTemplate.update(
				"UPDATE video_certification SET image_url = ?, submitted_at = CURRENT_TIMESTAMP WHERE certification_id = ?",
				imageUrl,
				certificationId);
	}

	public int countByClassIdAndStudentId(long classId, long studentId) {
		Integer count = jdbcTemplate.queryForObject(
				"""
						SELECT COUNT(DISTINCT vc.video_id)
						FROM video_certification vc
						INNER JOIN video_lesson vl ON vc.video_id = vl.video_id
						WHERE vl.class_id = ? AND vc.student_id = ?
						""",
				Integer.class,
				classId,
				studentId);
		return count != null ? count : 0;
	}
}
