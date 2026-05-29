package com.example.ams.api.clazz;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ams.api.dto.ClinicReservationActionRequest;
import com.example.ams.api.dto.ClinicWeekViewResponse;
import com.example.ams.common.ApiResponse;
import com.example.ams.service.ClinicReservationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/classes/{classId}/clinic")
public class ClinicReservationController {

	private final ClinicReservationService clinicReservationService;

	public ClinicReservationController(ClinicReservationService clinicReservationService) {
		this.clinicReservationService = clinicReservationService;
	}

	@GetMapping("/weeks/{weekStart}")
	public ApiResponse<ClinicWeekViewResponse> getWeek(
			@PathVariable long classId,
			@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
		return ApiResponse.ok(ClinicWeekViewResponse.from(clinicReservationService.getWeekView(classId, weekStart)));
	}

	@PutMapping("/reservations")
	public ApiResponse<ClinicWeekViewResponse> reserve(
			@PathVariable long classId,
			@Valid @RequestBody ClinicReservationActionRequest request) {
		return ApiResponse.ok(
				ClinicWeekViewResponse.from(clinicReservationService.reserveSlot(classId, request.slotId())));
	}

	@PutMapping("/reservations/cancel")
	public ApiResponse<ClinicWeekViewResponse> cancel(
			@PathVariable long classId,
			@Valid @RequestBody ClinicReservationActionRequest request) {
		return ApiResponse.ok(
				ClinicWeekViewResponse.from(clinicReservationService.cancelReservation(classId, request.slotId())));
	}
}
