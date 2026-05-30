package com.example.ams.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.example.ams.domain.clazz.AssistantClinicSlotRow;
import com.example.ams.domain.clazz.ClinicSlot;
import com.example.ams.domain.clazz.DayOfWeek;

@Repository
public class ClinicSlotRepository {

	private static final RowMapper<AssistantClinicSlotRow> ASSISTANT_WEEK_ROW_MAPPER = (rs, rowNum) -> new AssistantClinicSlotRow(
			new ClinicSlot(
					rs.getLong("slot_id"),
					rs.getLong("class_id"),
					rs.getDate("week_start_date").toLocalDate(),
					DayOfWeek.valueOf(rs.getString("day_of_week")),
					rs.getTime("start_time").toLocalTime(),
					rs.getObject("assistant_id", Long.class),
					rs.getString("assistant_name"),
					rs.getInt("max_capacity")),
			rs.getString("class_name"));

	private static final RowMapper<ClinicSlot> ROW_MAPPER = (rs, rowNum) -> new ClinicSlot(
			rs.getLong("slot_id"),
			rs.getLong("class_id"),
			rs.getDate("week_start_date").toLocalDate(),
			DayOfWeek.valueOf(rs.getString("day_of_week")),
			rs.getTime("start_time").toLocalTime(),
			rs.getObject("assistant_id", Long.class),
			rs.getString("assistant_name"),
			rs.getInt("max_capacity"));

	private static final String SELECT_BASE = """
			SELECT s.*, u.name AS assistant_name
			FROM clinic_slot s
			LEFT JOIN `user` u ON s.assistant_id = u.user_id
			""";

	private final JdbcTemplate jdbcTemplate;

	public ClinicSlotRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public boolean existsByClassIdAndAssistantId(long classId, long assistantId) {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM clinic_slot WHERE class_id = ? AND assistant_id = ?",
				Integer.class,
				classId,
				assistantId);
		return count != null && count > 0;
	}

	public List<AssistantClinicSlotRow> findByAssistantIdAndWeekStart(
			long assistantId,
			LocalDate weekStart,
			long academyId) {
		return jdbcTemplate.query(
				"""
						SELECT s.*, u.name AS assistant_name, c.name AS class_name
						FROM clinic_slot s
						LEFT JOIN `user` u ON s.assistant_id = u.user_id
						INNER JOIN `class` c ON s.class_id = c.class_id
						WHERE s.assistant_id = ? AND s.week_start_date = ? AND c.academy_id = ?
						ORDER BY FIELD(s.day_of_week, 'MON','TUE','WED','THU','FRI','SAT','SUN'), s.start_time, c.name
						""",
				ASSISTANT_WEEK_ROW_MAPPER,
				assistantId,
				weekStart,
				academyId);
	}

	public List<ClinicSlot> findByClassIdAndWeekStart(long classId, LocalDate weekStart) {
		return jdbcTemplate.query(
				SELECT_BASE
						+ """
								WHERE s.class_id = ? AND s.week_start_date = ?
								ORDER BY FIELD(s.day_of_week, 'MON','TUE','WED','THU','FRI','SAT','SUN'), s.start_time
								""",
				ROW_MAPPER,
				classId,
				weekStart);
	}

	public Optional<ClinicSlot> findById(long slotId) {
		return jdbcTemplate.query(SELECT_BASE + " WHERE s.slot_id = ?", ROW_MAPPER, slotId).stream().findFirst();
	}

	public Optional<ClinicSlot> findByIdAndClassId(long slotId, long classId) {
		return jdbcTemplate.query(
				SELECT_BASE + " WHERE s.slot_id = ? AND s.class_id = ?",
				ROW_MAPPER,
				slotId,
				classId).stream().findFirst();
	}

	public ClinicSlot insert(
			long classId,
			LocalDate weekStart,
			DayOfWeek dayOfWeek,
			LocalTime startTime,
			Long assistantId,
			int maxCapacity) {
		return insert(classId, weekStart, dayOfWeek, startTime, assistantId, maxCapacity, null);
	}

	public ClinicSlot insert(
			long classId,
			LocalDate weekStart,
			DayOfWeek dayOfWeek,
			LocalTime startTime,
			Long assistantId,
			int maxCapacity,
			Long lessonRecordId) {
		String sql = """
				INSERT INTO clinic_slot (class_id, week_start_date, day_of_week, start_time, assistant_id, max_capacity, lesson_record_id)
				VALUES (?, ?, ?, ?, ?, ?, ?)
				""";
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(connection -> {
			var ps = connection.prepareStatement(sql, new String[] { "slot_id" });
			ps.setLong(1, classId);
			ps.setDate(2, java.sql.Date.valueOf(weekStart));
			ps.setString(3, dayOfWeek.name());
			ps.setTime(4, java.sql.Time.valueOf(startTime));
			if (assistantId != null) {
				ps.setLong(5, assistantId);
			} else {
				ps.setNull(5, java.sql.Types.BIGINT);
			}
			ps.setInt(6, maxCapacity);
			if (lessonRecordId != null) {
				ps.setLong(7, lessonRecordId);
			} else {
				ps.setNull(7, java.sql.Types.BIGINT);
			}
			return ps;
		}, keyHolder);
		return findByIdAndClassId(keyHolder.getKey().longValue(), classId).orElseThrow();
	}

	public List<ClinicSlotSummary> findSummariesByLessonRecordId(long lessonRecordId) {
		return jdbcTemplate.query(
				"""
						SELECT s.slot_id, s.week_start_date, s.day_of_week, s.start_time, u.name AS assistant_name
						FROM clinic_slot s
						LEFT JOIN `user` u ON s.assistant_id = u.user_id
						WHERE s.lesson_record_id = ?
						ORDER BY s.slot_id
						""",
				(rs, rowNum) -> new ClinicSlotSummary(
						rs.getLong("slot_id"),
						rs.getDate("week_start_date").toLocalDate(),
						DayOfWeek.valueOf(rs.getString("day_of_week")),
						rs.getTime("start_time").toLocalTime(),
						rs.getString("assistant_name")),
				lessonRecordId);
	}

	public record ClinicSlotSummary(
			long slotId,
			LocalDate weekStartDate,
			DayOfWeek dayOfWeek,
			LocalTime startTime,
			String assistantName) {
	}

	public ClinicSlot update(
			long slotId,
			long classId,
			DayOfWeek dayOfWeek,
			LocalTime startTime,
			Long assistantId,
			int maxCapacity) {
		jdbcTemplate.update(
				"""
						UPDATE clinic_slot
						SET day_of_week = ?, start_time = ?, assistant_id = ?, max_capacity = ?
						WHERE slot_id = ? AND class_id = ?
						""",
				dayOfWeek.name(),
				java.sql.Time.valueOf(startTime),
				assistantId,
				maxCapacity,
				slotId,
				classId);
		return findByIdAndClassId(slotId, classId).orElseThrow();
	}

	public void delete(long slotId, long classId) {
		jdbcTemplate.update("DELETE FROM clinic_slot WHERE slot_id = ? AND class_id = ?", slotId, classId);
	}
}
