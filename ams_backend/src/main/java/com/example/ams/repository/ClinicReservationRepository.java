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

import com.example.ams.domain.clazz.ClinicReservation;
import com.example.ams.domain.clazz.ClinicReservationStatus;
import com.example.ams.domain.clazz.ClinicReservationWithSlot;
import com.example.ams.domain.clazz.DayOfWeek;

@Repository
public class ClinicReservationRepository {

	private static final RowMapper<ClinicReservation> ROW_MAPPER = (rs, rowNum) -> new ClinicReservation(
			rs.getLong("reservation_id"),
			rs.getLong("slot_id"),
			rs.getLong("student_id"),
			rs.getString("student_name"),
			ClinicReservationStatus.valueOf(rs.getString("status")),
			rs.getObject("result_attended") != null ? rs.getBoolean("result_attended") : null,
			rs.getString("result_memo"),
			rs.getTimestamp("created_at").toInstant());

	private static final String SELECT_BASE = """
			SELECT r.*, u.name AS student_name
			FROM clinic_reservation r
			INNER JOIN `user` u ON r.student_id = u.user_id
			""";

	private static final RowMapper<ClinicReservationWithSlot> WITH_SLOT_ROW_MAPPER = (rs, rowNum) -> new ClinicReservationWithSlot(
			new ClinicReservation(
					rs.getLong("reservation_id"),
					rs.getLong("slot_id"),
					rs.getLong("student_id"),
					rs.getString("student_name"),
					ClinicReservationStatus.valueOf(rs.getString("status")),
					rs.getObject("result_attended") != null ? rs.getBoolean("result_attended") : null,
					rs.getString("result_memo"),
					rs.getTimestamp("created_at").toInstant()),
			rs.getDate("week_start_date").toLocalDate(),
			DayOfWeek.valueOf(rs.getString("day_of_week")),
			rs.getTime("start_time").toLocalTime());

	private final JdbcTemplate jdbcTemplate;

	public ClinicReservationRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public int countBySlotId(long slotId) {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM clinic_reservation WHERE slot_id = ?",
				Integer.class,
				slotId);
		return count != null ? count : 0;
	}

	public List<ClinicReservation> findBySlotId(long slotId) {
		return jdbcTemplate.query(SELECT_BASE + " WHERE r.slot_id = ? ORDER BY u.name ASC", ROW_MAPPER, slotId);
	}

	public List<ClinicReservation> findBySlotIds(List<Long> slotIds) {
		if (slotIds.isEmpty()) {
			return List.of();
		}
		String in = String.join(",", slotIds.stream().map(id -> "?").toList());
		return jdbcTemplate.query(
				SELECT_BASE + " WHERE r.slot_id IN (" + in + ") ORDER BY r.slot_id, u.name ASC",
				ROW_MAPPER,
				slotIds.toArray());
	}

	public boolean existsStudentReservationAtSameTimeExcludingSlot(
			long classId,
			long studentId,
			LocalDate weekStart,
			DayOfWeek dayOfWeek,
			LocalTime startTime,
			long excludeSlotId) {
		Integer count = jdbcTemplate.queryForObject(
				"""
						SELECT COUNT(*) FROM clinic_reservation r
						INNER JOIN clinic_slot s ON r.slot_id = s.slot_id
						WHERE s.class_id = ? AND s.week_start_date = ?
						  AND s.day_of_week = ? AND s.start_time = ?
						  AND r.student_id = ? AND s.slot_id <> ?
						""",
				Integer.class,
				classId,
				weekStart,
				dayOfWeek.name(),
				startTime,
				studentId,
				excludeSlotId);
		return count != null && count > 0;
	}

	public Optional<ClinicReservation> findBySlotIdAndStudentId(long slotId, long studentId) {
		return jdbcTemplate.query(
				SELECT_BASE + " WHERE r.slot_id = ? AND r.student_id = ?",
				ROW_MAPPER,
				slotId,
				studentId).stream().findFirst();
	}

	public Optional<ClinicReservation> findById(long reservationId) {
		return jdbcTemplate.query(SELECT_BASE + " WHERE r.reservation_id = ?", ROW_MAPPER, reservationId)
				.stream()
				.findFirst();
	}

	public ClinicReservation insert(long slotId, long studentId) {
		String sql = "INSERT INTO clinic_reservation (slot_id, student_id, status) VALUES (?, ?, 'RESERVED')";
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(connection -> {
			var ps = connection.prepareStatement(sql, new String[] { "reservation_id" });
			ps.setLong(1, slotId);
			ps.setLong(2, studentId);
			return ps;
		}, keyHolder);
		return findById(keyHolder.getKey().longValue()).orElseThrow();
	}

	public void delete(long reservationId) {
		jdbcTemplate.update("DELETE FROM clinic_reservation WHERE reservation_id = ?", reservationId);
	}

	public void deleteBySlotIdAndStudentId(long slotId, long studentId) {
		jdbcTemplate.update("DELETE FROM clinic_reservation WHERE slot_id = ? AND student_id = ?", slotId, studentId);
	}

	public List<ClinicReservation> findByClassIdAndStudentId(long classId, long studentId) {
		return jdbcTemplate.query(
				SELECT_BASE
						+ """
								INNER JOIN clinic_slot s ON r.slot_id = s.slot_id
								WHERE s.class_id = ? AND r.student_id = ?
								ORDER BY s.week_start_date DESC, s.day_of_week, s.start_time
								""",
				ROW_MAPPER,
				classId,
				studentId);
	}

	public List<ClinicReservationWithSlot> findByClassIdAndStudentIdWithSlot(long classId, long studentId) {
		return jdbcTemplate.query(
				"""
						SELECT r.*, u.name AS student_name,
						       s.week_start_date, s.day_of_week, s.start_time
						FROM clinic_reservation r
						INNER JOIN `user` u ON r.student_id = u.user_id
						INNER JOIN clinic_slot s ON r.slot_id = s.slot_id
						WHERE s.class_id = ? AND r.student_id = ?
						ORDER BY s.week_start_date, s.day_of_week, s.start_time
						""",
				WITH_SLOT_ROW_MAPPER,
				classId,
				studentId);
	}

	public ClinicReservation updateResult(long reservationId, Boolean attended, String memo) {
		jdbcTemplate.update(
				"UPDATE clinic_reservation SET result_attended = ?, result_memo = ? WHERE reservation_id = ?",
				attended,
				memo,
				reservationId);
		return findById(reservationId).orElseThrow();
	}
}
