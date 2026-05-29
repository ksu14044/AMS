package com.example.ams.repository;

import java.time.LocalTime;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.example.ams.domain.clazz.ClassScheduleSlot;
import com.example.ams.domain.clazz.DayOfWeek;

@Repository
public class ClassScheduleRepository {

	private static final RowMapper<ClassScheduleSlot> ROW_MAPPER = (rs, rowNum) -> new ClassScheduleSlot(
			rs.getLong("schedule_id"),
			rs.getLong("class_id"),
			DayOfWeek.valueOf(rs.getString("day_of_week")),
			rs.getTime("start_time").toLocalTime(),
			rs.getTime("end_time").toLocalTime(),
			rs.getString("room"));

	private final JdbcTemplate jdbcTemplate;

	public ClassScheduleRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<ClassScheduleSlot> findByClassId(long classId) {
		return jdbcTemplate.query(
				"""
						SELECT * FROM class_schedule
						WHERE class_id = ?
						ORDER BY FIELD(day_of_week, 'MON','TUE','WED','THU','FRI','SAT','SUN'), start_time
						""",
				ROW_MAPPER,
				classId);
	}

	public void deleteByClassId(long classId) {
		jdbcTemplate.update("DELETE FROM class_schedule WHERE class_id = ?", classId);
	}

	public void insert(long classId, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime, String room) {
		jdbcTemplate.update(
				"""
						INSERT INTO class_schedule (class_id, day_of_week, start_time, end_time, room)
						VALUES (?, ?, ?, ?, ?)
						""",
				classId,
				dayOfWeek.name(),
				startTime,
				endTime,
				room);
	}
}
