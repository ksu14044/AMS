package com.example.ams.api.clazz;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.ams.api.dto.ClinicSlotResponse;
import com.example.ams.api.dto.CreateClinicSlotRequest;
import com.example.ams.api.dto.UpdateClinicSlotRequest;
import com.example.ams.api.dto.UserResponse;
import com.example.ams.common.ApiResponse;
import com.example.ams.service.ClinicSlotService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/classes/{classId}/clinic")
public class ClassClinicController {

	private final ClinicSlotService clinicSlotService;

	public ClassClinicController(ClinicSlotService clinicSlotService) {
		this.clinicSlotService = clinicSlotService;
	}

	@GetMapping("/slots")
	public ApiResponse<List<ClinicSlotResponse>> listSlots(
			@PathVariable long classId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
		return ApiResponse.ok(clinicSlotService.listSlotResponses(classId, weekStart));
	}

	@GetMapping("/assistants")
	public ApiResponse<List<UserResponse>> listAssistants(@PathVariable long classId) {
		List<UserResponse> assistants = clinicSlotService.listAssistants(classId).stream()
				.map(UserResponse::from)
				.toList();
		return ApiResponse.ok(assistants);
	}

	@PostMapping("/slots")
	public ApiResponse<ClinicSlotResponse> createSlot(
			@PathVariable long classId,
			@Valid @RequestBody CreateClinicSlotRequest request) {
		int capacity = request.maxCapacity() != null ? request.maxCapacity() : 10;
		return ApiResponse.ok(clinicSlotService.toResponse(clinicSlotService.createSlot(
				classId,
				request.weekStartDate(),
				request.dayOfWeek(),
				request.startTime(),
				request.assistantId(),
				request.presetId(),
				capacity,
				request.targetStudentIds())));
	}

	@PatchMapping("/slots/{slotId}")
	public ApiResponse<ClinicSlotResponse> updateSlot(
			@PathVariable long classId,
			@PathVariable long slotId,
			@Valid @RequestBody UpdateClinicSlotRequest request) {
		int capacity = request.maxCapacity() != null ? request.maxCapacity() : 10;
		return ApiResponse.ok(clinicSlotService.toResponse(clinicSlotService.updateSlot(
				classId,
				slotId,
				request.dayOfWeek(),
				request.startTime(),
				request.assistantId(),
				request.presetId(),
				capacity)));
	}

	@DeleteMapping("/slots/{slotId}")
	public ApiResponse<Void> deleteSlot(@PathVariable long classId, @PathVariable long slotId) {
		clinicSlotService.deleteSlot(classId, slotId);
		return ApiResponse.ok(null);
	}
}
