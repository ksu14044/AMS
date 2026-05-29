package com.example.ams.api.admin;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ams.api.dto.CreateSignupInviteRequest;
import com.example.ams.api.dto.SignupInviteResponse;
import com.example.ams.common.ApiResponse;
import com.example.ams.common.BusinessException;
import com.example.ams.common.ErrorCode;
import com.example.ams.domain.auth.SignupInviteKind;
import com.example.ams.security.CurrentUserService;
import com.example.ams.service.SignupInviteService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/signup-invites")
@PreAuthorize("hasAnyRole('ACADEMY_ADMIN', 'STAFF_OFFICE')")
public class AdminSignupInviteController {

	private final SignupInviteService signupInviteService;
	private final CurrentUserService currentUserService;

	public AdminSignupInviteController(
			SignupInviteService signupInviteService,
			CurrentUserService currentUserService) {
		this.signupInviteService = signupInviteService;
		this.currentUserService = currentUserService;
	}

	@PostMapping
	public ApiResponse<SignupInviteResponse> create(@Valid @RequestBody CreateSignupInviteRequest request) {
		long academyId = currentUserService.requireAcademyId();
		return switch (request.kind()) {
			case STAFF -> {
				if (request.role() == null) {
					throw new BusinessException(ErrorCode.INVALID_REQUEST, "교직원 가입 링크는 역할이 필요합니다.");
				}
				yield ApiResponse.ok(signupInviteService.createStaffInvite(academyId, request.role()));
			}
			case STUDENT -> ApiResponse.ok(signupInviteService.createStudentInvite(academyId));
			case ACADEMY -> throw new BusinessException(
					ErrorCode.INVALID_REQUEST,
					"학원 개설 링크는 플랫폼에서 발급합니다.");
		};
	}
}
