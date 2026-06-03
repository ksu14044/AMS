package com.example.ams.api.auth;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.ams.api.dto.LoginRequest;
import com.example.ams.api.dto.LoginResponse;
import com.example.ams.api.dto.LoginSelectRequest;
import com.example.ams.api.dto.RefreshTokenRequest;
import com.example.ams.api.dto.SignupAcademyRequest;
import com.example.ams.api.dto.SignupInvitePreviewResponse;
import com.example.ams.api.dto.SignupInviteResponse;
import com.example.ams.api.dto.SignupParentRequest;
import com.example.ams.api.dto.SignupStaffRequest;
import com.example.ams.api.dto.SignupStudentRequest;
import com.example.ams.api.dto.TokenResponse;
import com.example.ams.common.ApiResponse;
import com.example.ams.domain.auth.SignupInvitePayload;
import com.example.ams.domain.user.User;
import com.example.ams.repository.AcademyRepository;
import com.example.ams.service.AuthService;
import com.example.ams.service.LoginOutcome;
import com.example.ams.service.SignupInviteService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;
	private final SignupInviteService signupInviteService;
	private final AcademyRepository academyRepository;

	public AuthController(
			AuthService authService,
			SignupInviteService signupInviteService,
			AcademyRepository academyRepository) {
		this.authService = authService;
		this.signupInviteService = signupInviteService;
		this.academyRepository = academyRepository;
	}

	@GetMapping("/signup/invite")
	public ApiResponse<SignupInvitePreviewResponse> previewSignupInvite(@RequestParam String token) {
		SignupInvitePayload invite = signupInviteService.parse(token);
		String academyName = null;
		if (invite.academyId() != null) {
			academyName = academyRepository.findById(invite.academyId())
					.map(a -> a.name())
					.orElse(null);
		}
		return ApiResponse.ok(SignupInvitePreviewResponse.of(
				invite,
				academyName,
				signupInviteService.roleLabel(invite.role())));
	}

	@PostMapping("/signup-invites/academy")
	public ApiResponse<SignupInviteResponse> createAcademySignupInvite(
			@RequestHeader("X-Ams-Bootstrap-Key") String bootstrapKey) {
		signupInviteService.assertAcademyBootstrapKey(bootstrapKey);
		return ApiResponse.ok(signupInviteService.createAcademyInvite());
	}

	@PostMapping("/signup/academy")
	public ApiResponse<TokenResponse> signupAcademy(@Valid @RequestBody SignupAcademyRequest request) {
		User user = authService.signupAcademy(
				request.inviteToken(),
				request.academyName(),
				request.academyCode(),
				request.adminName(),
				request.email(),
				request.password(),
				request.phoneNumber());
		return ApiResponse.ok(toTokenResponse(user));
	}

	@PostMapping("/signup/staff")
	public ApiResponse<TokenResponse> signupStaff(@Valid @RequestBody SignupStaffRequest request) {
		User user = authService.signupStaff(
				request.inviteToken(),
				request.academyCode(),
				request.name(),
				request.email(),
				request.password(),
				request.phoneNumber(),
				request.role(),
				request.subject());
		return ApiResponse.ok(toTokenResponse(user));
	}

	@PostMapping("/signup/parent")
	public ApiResponse<TokenResponse> signupParent(@Valid @RequestBody SignupParentRequest request) {
		User user = authService.signupParent(
				request.inviteToken(),
				request.academyCode(),
				request.name(),
				request.email(),
				request.password(),
				request.phoneNumber());
		return ApiResponse.ok(toTokenResponse(user));
	}

	@PostMapping("/signup/student")
	public ApiResponse<TokenResponse> signupStudent(@Valid @RequestBody SignupStudentRequest request) {
		User user = authService.signupStudent(
				request.inviteToken(),
				request.academyCode(),
				request.name(),
				request.email(),
				request.password(),
				request.phoneNumber());
		return ApiResponse.ok(toTokenResponse(user));
	}

	@PostMapping("/login")
	public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		LoginOutcome outcome = authService.login(request.email(), request.password());
		return ApiResponse.ok(toLoginResponse(outcome));
	}

	@PostMapping("/login/select")
	public ApiResponse<TokenResponse> loginSelect(@Valid @RequestBody LoginSelectRequest request) {
		User user = authService.completeLogin(request.loginToken(), request.userId());
		return ApiResponse.ok(toTokenResponse(user));
	}

	@PostMapping("/refresh")
	public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
		User user = authService.refresh(request.refreshToken());
		return ApiResponse.ok(toTokenResponse(user));
	}

	private LoginResponse toLoginResponse(LoginOutcome outcome) {
		if (outcome instanceof LoginOutcome.Authenticated authenticated) {
			return LoginResponse.authenticated(toTokenResponse(authenticated.user()));
		}
		LoginOutcome.AcademySelectionRequired selection = (LoginOutcome.AcademySelectionRequired) outcome;
		return LoginResponse.academySelectionRequired(selection.loginToken(), selection.academies());
	}

	private TokenResponse toTokenResponse(User user) {
		return TokenResponse.of(
				authService.createAccessToken(user),
				authService.createRefreshToken(user),
				user);
	}
}
