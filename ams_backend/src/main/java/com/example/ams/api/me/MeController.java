package com.example.ams.api.me;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ams.api.dto.MyAssistantClinicWeekResponse;
import com.example.ams.api.dto.PendingTaskResponse;
import com.example.ams.api.dto.TokenResponse;
import com.example.ams.common.ApiResponse;
import com.example.ams.common.BusinessException;
import com.example.ams.common.ErrorCode;
import com.example.ams.domain.user.User;
import com.example.ams.domain.user.UserRole;
import com.example.ams.repository.UserRepository;
import com.example.ams.security.CurrentUserService;
import com.example.ams.service.ClinicReservationService;
import com.example.ams.service.PendingTaskService;

@RestController
@RequestMapping("/api/v1/me")
public class MeController {

	private final UserRepository userRepository;
	private final CurrentUserService currentUserService;
	private final ClinicReservationService clinicReservationService;
	private final PendingTaskService pendingTaskService;

	public MeController(
			UserRepository userRepository,
			CurrentUserService currentUserService,
			ClinicReservationService clinicReservationService,
			PendingTaskService pendingTaskService) {
		this.userRepository = userRepository;
		this.currentUserService = currentUserService;
		this.clinicReservationService = clinicReservationService;
		this.pendingTaskService = pendingTaskService;
	}

	@GetMapping
	public ApiResponse<TokenResponse.UserSummary> me() {
		User user = userRepository.findById(currentUserService.requireUserId())
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		currentUserService.assertSameAcademy(user.academyId());
		return ApiResponse.ok(TokenResponse.UserSummary.from(user));
	}

	@GetMapping("/clinic/weeks/{weekStart}")
	public ApiResponse<MyAssistantClinicWeekResponse> myClinicWeek(
			@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
		return ApiResponse.ok(
				MyAssistantClinicWeekResponse.from(clinicReservationService.getMyAssistantWeekView(weekStart)));
	}

	@GetMapping("/pending-tasks")
	public ApiResponse<List<PendingTaskResponse>> pendingTasks() {
		if (currentUserService.requireRole() != UserRole.STUDENT) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		List<PendingTaskResponse> items = pendingTaskService
				.listPending(currentUserService.requireUserId()).stream()
				.map(PendingTaskResponse::from)
				.toList();
		return ApiResponse.ok(items);
	}
}
