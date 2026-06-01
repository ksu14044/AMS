package com.example.ams.api.dto;

import java.time.Instant;
import java.util.Map;

import com.example.ams.common.ClinicResultSchemaJson;
import com.example.ams.domain.clazz.ClinicReservation;

public record ClinicReservationResponse(
		long reservationId,
		long slotId,
		long studentId,
		String studentName,
		com.example.ams.domain.clazz.ClinicReservationStatus status,
		Boolean resultAttended,
		String resultMemo,
		Map<String, Object> result,
		Instant resultSavedAt) {

	public static ClinicReservationResponse from(ClinicReservation r) {
		return new ClinicReservationResponse(
				r.reservationId(),
				r.slotId(),
				r.studentId(),
				r.studentName(),
				r.status(),
				r.resultAttended(),
				r.resultMemo(),
				ClinicResultSchemaJson.fromValuesJson(r.resultJson()),
				r.resultSavedAt());
	}
}
