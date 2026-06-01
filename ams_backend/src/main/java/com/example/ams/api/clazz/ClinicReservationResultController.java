package com.example.ams.api.clazz;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ams.api.dto.ClinicReservationResponse;
import com.example.ams.api.dto.UpdateClinicReservationResultRequest;
import com.example.ams.common.ApiResponse;
import com.example.ams.service.ClinicReservationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/clinic/reservations")
public class ClinicReservationResultController {

	private final ClinicReservationService clinicReservationService;

	public ClinicReservationResultController(ClinicReservationService clinicReservationService) {
		this.clinicReservationService = clinicReservationService;
	}

	@PatchMapping("/{reservationId}/result")
	public ApiResponse<ClinicReservationResponse> updateResult(
			@PathVariable long reservationId,
			@Valid @RequestBody UpdateClinicReservationResultRequest request) {
		return ApiResponse.ok(ClinicReservationResponse.from(
				clinicReservationService.updateResult(reservationId, request.result())));
	}
}
