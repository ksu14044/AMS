package com.example.ams.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.example.ams.common.BusinessException;
import com.example.ams.common.ClinicResultSchemaJson;
import com.example.ams.common.ErrorCode;
import com.example.ams.domain.clazz.ClinicResultPreset;

@Repository
public class ClinicResultPresetRepository {

	private static final RowMapper<ClinicResultPreset> ROW_MAPPER = (rs, rowNum) -> new ClinicResultPreset(
			rs.getLong("preset_id"),
			rs.getLong("class_id"),
			rs.getString("name"),
			rs.getString("field_schema"));

	private final JdbcTemplate jdbcTemplate;

	public ClinicResultPresetRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<ClinicResultPreset> findByClassId(long classId) {
		return jdbcTemplate.query(
				"""
						SELECT preset_id, class_id, name, field_schema
						FROM clinic_result_preset
						WHERE class_id = ?
						ORDER BY preset_id ASC
						""",
				ROW_MAPPER,
				classId);
	}

	public Optional<ClinicResultPreset> findById(long presetId) {
		return jdbcTemplate.query(
				"SELECT preset_id, class_id, name, field_schema FROM clinic_result_preset WHERE preset_id = ?",
				ROW_MAPPER,
				presetId).stream().findFirst();
	}

	public Optional<ClinicResultPreset> findByIdAndClassId(long presetId, long classId) {
		return jdbcTemplate.query(
				"""
						SELECT preset_id, class_id, name, field_schema
						FROM clinic_result_preset
						WHERE preset_id = ? AND class_id = ?
						""",
				ROW_MAPPER,
				presetId,
				classId).stream().findFirst();
	}

	public ClinicResultPreset insert(long classId, String name, String fieldSchemaJson) {
		String sql = """
				INSERT INTO clinic_result_preset (class_id, name, field_schema)
				VALUES (?, ?, ?)
				""";
		KeyHolder keyHolder = new GeneratedKeyHolder();
		try {
			jdbcTemplate.update(connection -> {
				var ps = connection.prepareStatement(sql, new String[] { "preset_id" });
				ps.setLong(1, classId);
				ps.setString(2, name);
				ps.setString(3, fieldSchemaJson);
				return ps;
			}, keyHolder);
		} catch (DuplicateKeyException ex) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "같은 이름의 프리셋이 이미 있습니다.");
		}
		return findById(keyHolder.getKey().longValue()).orElseThrow();
	}

	public ClinicResultPreset update(long presetId, long classId, String name, String fieldSchemaJson) {
		try {
			int updated = jdbcTemplate.update(
					"""
							UPDATE clinic_result_preset
							SET name = ?, field_schema = ?
							WHERE preset_id = ? AND class_id = ?
							""",
					name,
					fieldSchemaJson,
					presetId,
					classId);
			if (updated == 0) {
				throw new BusinessException(ErrorCode.INVALID_REQUEST, "클리닉 프리셋을 찾을 수 없습니다.");
			}
		} catch (DuplicateKeyException ex) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "같은 이름의 프리셋이 이미 있습니다.");
		}
		return findByIdAndClassId(presetId, classId).orElseThrow();
	}

	public void delete(long presetId, long classId) {
		int updated = jdbcTemplate.update(
				"DELETE FROM clinic_result_preset WHERE preset_id = ? AND class_id = ?",
				presetId,
				classId);
		if (updated == 0) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "클리닉 프리셋을 찾을 수 없습니다.");
		}
	}

	public int countSlotsUsingPreset(long presetId) {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM clinic_slot WHERE clinic_result_preset_id = ?",
				Integer.class,
				presetId);
		return count != null ? count : 0;
	}

	public ClinicResultPreset ensureDefaultPreset(long classId) {
		return findByClassId(classId).stream()
				.filter(p -> "기본".equals(p.name()))
				.findFirst()
				.orElseGet(() -> insert(classId, "기본", ClinicResultSchemaJson.DEFAULT_SCHEMA_JSON));
	}
}
