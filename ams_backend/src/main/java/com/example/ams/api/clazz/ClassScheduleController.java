package com.example.ams.api.clazz;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ams.api.dto.ScheduleSlotResponse;
import com.example.ams.api.dto.UpdateClassScheduleRequest;
import com.example.ams.common.ApiResponse;
import com.example.ams.service.ClassScheduleService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/classes/{classId}/schedule")
public class ClassScheduleController {

	private final ClassScheduleService classScheduleService;

	public ClassScheduleController(ClassScheduleService classScheduleService) {
		this.classScheduleService = classScheduleService;
	}

	@GetMapping
	public ApiResponse<List<ScheduleSlotResponse>> getSchedule(@PathVariable long classId) {
		List<ScheduleSlotResponse> slots = classScheduleService.getSchedule(classId).stream()
				.map(ScheduleSlotResponse::from)
				.toList();
		return ApiResponse.ok(slots);
	}

	@PatchMapping
	public ApiResponse<List<ScheduleSlotResponse>> updateSchedule(
			@PathVariable long classId,
			@Valid @RequestBody UpdateClassScheduleRequest request) {
		List<ScheduleSlotResponse> slots = classScheduleService
				.replaceSchedule(classId, request.slots())
				.stream()
				.map(ScheduleSlotResponse::from)
				.toList();
		return ApiResponse.ok(slots);
	}
}
