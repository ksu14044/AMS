package com.example.ams.api.me;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ams.api.dto.MyAssistantClinicWeekResponse;
import com.example.ams.api.dto.TokenResponse;
import com.example.ams.common.ApiResponse;
import com.example.ams.common.BusinessException;
import com.example.ams.common.ErrorCode;
import com.example.ams.domain.user.User;
import com.example.ams.repository.UserRepository;
import com.example.ams.security.CurrentUserService;
import com.example.ams.service.ClinicReservationService;

@RestController
@RequestMapping("/api/v1/me")
public class MeController {

	private final UserRepository userRepository;
	private final CurrentUserService currentUserService;
	private final ClinicReservationService clinicReservationService;

	public MeController(
			UserRepository userRepository,
			CurrentUserService currentUserService,
			ClinicReservationService clinicReservationService) {
		this.userRepository = userRepository;
		this.currentUserService = currentUserService;
		this.clinicReservationService = clinicReservationService;
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
}
