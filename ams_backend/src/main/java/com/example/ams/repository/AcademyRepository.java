package com.example.ams.repository;

import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.example.ams.domain.academy.Academy;

@Repository
public class AcademyRepository {

	private static final RowMapper<Academy> ROW_MAPPER = (rs, rowNum) -> new Academy(
			rs.getLong("academy_id"),
			rs.getString("name"),
			rs.getString("code"),
			rs.getTimestamp("created_at").toInstant());

	private final JdbcTemplate jdbcTemplate;

	public AcademyRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Optional<Academy> findByCode(String code) {
		String sql = "SELECT academy_id, name, code, created_at FROM academy WHERE code = ?";
		return jdbcTemplate.query(sql, ROW_MAPPER, code).stream().findFirst();
	}

	public Optional<Academy> findById(long academyId) {
		String sql = "SELECT academy_id, name, code, created_at FROM academy WHERE academy_id = ?";
		return jdbcTemplate.query(sql, ROW_MAPPER, academyId).stream().findFirst();
	}

	public Academy insert(String name, String code) {
		String sql = "INSERT INTO academy (name, code) VALUES (?, ?)";
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(connection -> {
			var ps = connection.prepareStatement(sql, new String[] { "academy_id" });
			ps.setString(1, name);
			ps.setString(2, code);
			return ps;
		}, keyHolder);
		long id = keyHolder.getKey().longValue();
		return findById(id).orElseThrow();
	}

	public boolean existsByCode(String code) {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM academy WHERE code = ?", Integer.class, code);
		return count != null && count > 0;
	}
}
