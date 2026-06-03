package com.example.ams.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.example.ams.domain.link.ParentStudentLink;

@Repository
public class ParentStudentLinkRepository {

	private static final RowMapper<ParentStudentLink> ROW_MAPPER = (rs, rowNum) -> new ParentStudentLink(
			rs.getLong("link_id"),
			rs.getLong("parent_id"),
			rs.getLong("student_id"),
			rs.getLong("linked_by"),
			rs.getTimestamp("linked_at").toInstant());

	private final JdbcTemplate jdbcTemplate;

	public ParentStudentLinkRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Optional<ParentStudentLink> findById(long linkId) {
		return jdbcTemplate.query("SELECT * FROM parent_student_link WHERE link_id = ?", ROW_MAPPER, linkId)
				.stream()
				.findFirst();
	}

	public boolean existsByParentIdAndStudentId(long parentId, long studentId) {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM parent_student_link WHERE parent_id = ? AND student_id = ?",
				Integer.class,
				parentId,
				studentId);
		return count != null && count > 0;
	}

	public ParentStudentLink insert(long parentId, long studentId, long linkedBy) {
		String sql = """
				INSERT INTO parent_student_link (parent_id, student_id, linked_by)
				VALUES (?, ?, ?)
				""";
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(
				con -> {
					var ps = con.prepareStatement(sql, new String[] { "link_id" });
					ps.setLong(1, parentId);
					ps.setLong(2, studentId);
					ps.setLong(3, linkedBy);
					return ps;
				},
				keyHolder);
		long linkId = keyHolder.getKey().longValue();
		return findById(linkId).orElseThrow();
	}

	public void deleteById(long linkId) {
		jdbcTemplate.update("DELETE FROM parent_student_link WHERE link_id = ?", linkId);
	}

	public List<ParentStudentLink> findByStudentId(long studentId) {
		return jdbcTemplate.query(
				"SELECT * FROM parent_student_link WHERE student_id = ? ORDER BY linked_at DESC",
				ROW_MAPPER,
				studentId);
	}

	public List<ParentStudentLink> findByParentId(long parentId) {
		return jdbcTemplate.query(
				"SELECT * FROM parent_student_link WHERE parent_id = ? ORDER BY linked_at DESC",
				ROW_MAPPER,
				parentId);
	}
}
