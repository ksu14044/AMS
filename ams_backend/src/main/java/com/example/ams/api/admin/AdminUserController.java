package com.example.ams.api.admin;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ams.api.dto.ApproveUserRequest;
import com.example.ams.api.dto.TokenResponse;
import com.example.ams.api.dto.UserResponse;
import com.example.ams.common.ApiResponse;
import com.example.ams.domain.user.User;
import com.example.ams.service.AdminUserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ACADEMY_ADMIN')")
public class AdminUserController {

	private final AdminUserService adminUserService;

	public AdminUserController(AdminUserService adminUserService) {
		this.adminUserService = adminUserService;
	}

	@GetMapping("/pending")
	public ApiResponse<List<UserResponse>> listPending() {
		List<UserResponse> users = adminUserService.listPendingStaff().stream()
				.map(UserResponse::from)
				.toList();
		return ApiResponse.ok(users);
	}

	@PostMapping("/{id}/approve")
	public ApiResponse<TokenResponse.UserSummary> approve(
			@PathVariable long id,
			@Valid @RequestBody ApproveUserRequest request) {
		User approved = adminUserService.approve(id, request.role(), request.subject());
		return ApiResponse.ok(TokenResponse.UserSummary.from(approved));
	}
}
