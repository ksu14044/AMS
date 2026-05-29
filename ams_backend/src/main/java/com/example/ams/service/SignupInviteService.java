package com.example.ams.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.example.ams.common.BusinessException;
import com.example.ams.common.ErrorCode;
import com.example.ams.config.JwtProperties;
import com.example.ams.config.SignupProperties;
import com.example.ams.api.dto.SignupInviteResponse;
import com.example.ams.domain.academy.Academy;
import com.example.ams.domain.auth.SignupInviteKind;
import com.example.ams.domain.auth.SignupInvitePayload;
import com.example.ams.domain.user.Subject;
import com.example.ams.domain.user.UserRole;
import com.example.ams.repository.AcademyRepository;
import com.example.ams.security.JwtService;

@Service
public class SignupInviteService {

	private final JwtService jwtService;
	private final JwtProperties jwtProperties;
	private final SignupProperties signupProperties;
	private final AcademyRepository academyRepository;

	public SignupInviteService(
			JwtService jwtService,
			JwtProperties jwtProperties,
			SignupProperties signupProperties,
			AcademyRepository academyRepository) {
		this.jwtService = jwtService;
		this.jwtProperties = jwtProperties;
		this.signupProperties = signupProperties;
		this.academyRepository = academyRepository;
	}

	public SignupInvitePayload parse(String token) {
		SignupInvitePayload invite;
		try {
			invite = jwtService.parseSignupInviteToken(token);
		} catch (Exception ex) {
			throw new BusinessException(ErrorCode.INVALID_SIGNUP_INVITE);
		}
		assertNotExpired(invite);
		return invite;
	}

	public SignupInvitePayload requireKind(String token, SignupInviteKind expected) {
		SignupInvitePayload invite = parse(token);
		if (invite.kind() != expected) {
			throw new BusinessException(ErrorCode.INVALID_SIGNUP_INVITE);
		}
		return invite;
	}

	private void assertNotExpired(SignupInvitePayload invite) {
		if (invite.expiresAt().isBefore(Instant.now())) {
			throw new BusinessException(ErrorCode.INVALID_SIGNUP_INVITE);
		}
	}

	public Academy requireAcademy(SignupInvitePayload invite) {
		if (invite.academyId() == null) {
			throw new BusinessException(ErrorCode.INVALID_SIGNUP_INVITE);
		}
		return academyRepository.findById(invite.academyId())
				.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_SIGNUP_INVITE));
	}

	public void assertStaffRoleMatches(SignupInvitePayload invite, UserRole requestRole) {
		if (invite.role() != requestRole) {
			throw new BusinessException(ErrorCode.INVALID_SIGNUP_INVITE, "가입 링크의 역할과 일치하지 않습니다.");
		}
	}

	public SignupInviteResponse createAcademyInvite() {
		Instant expiresAt = Instant.now().plusMillis(jwtProperties.signupInviteExpirationMs());
		SignupInvitePayload payload = new SignupInvitePayload(
				SignupInviteKind.ACADEMY,
				null,
				null,
				null,
				null,
				expiresAt);
		return toResponse(payload);
	}

	public SignupInviteResponse createStaffInvite(long academyId, UserRole role) {
		Academy academy = academyRepository.findById(academyId)
				.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST));
		if (!role.isStaffSignupRole()) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "교직원 가입 링크에 허용되지 않은 역할입니다.");
		}
		Subject subject = defaultSubjectForRole(role);
		Instant expiresAt = Instant.now().plusMillis(jwtProperties.signupInviteExpirationMs());
		SignupInvitePayload payload = new SignupInvitePayload(
				SignupInviteKind.STAFF,
				academy.academyId(),
				academy.code(),
				role,
				subject,
				expiresAt);
		return toResponse(payload);
	}

	public SignupInviteResponse createStudentInvite(long academyId) {
		Academy academy = academyRepository.findById(academyId)
				.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST));
		Instant expiresAt = Instant.now().plusMillis(jwtProperties.signupInviteExpirationMs());
		SignupInvitePayload payload = new SignupInvitePayload(
				SignupInviteKind.STUDENT,
				academy.academyId(),
				academy.code(),
				UserRole.STUDENT,
				null,
				expiresAt);
		return toResponse(payload);
	}

	public boolean isAcademyBootstrapEnabled() {
		String key = signupProperties.academyBootstrapKey();
		return key != null && !key.isBlank();
	}

	public void assertAcademyBootstrapKey(String providedKey) {
		if (!isAcademyBootstrapEnabled()) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "학원 개설 링크 발급이 비활성화되어 있습니다.");
		}
		if (!signupProperties.academyBootstrapKey().equals(providedKey)) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
	}

	public String buildSignupPath(SignupInviteKind kind) {
		return switch (kind) {
			case ACADEMY -> "/signup/academy";
			case STAFF -> "/signup/staff";
			case STUDENT -> "/signup/student";
		};
	}

	public String roleLabel(UserRole role) {
		if (role == null) {
			return null;
		}
		return switch (role) {
			case TEACHER_KO -> "국어 선생님";
			case TEACHER_EN -> "영어 선생님";
			case TEACHER_MATH -> "수학 선생님";
			case STAFF_OFFICE -> "행정";
			case ASSISTANT_KO -> "국어 조교";
			case ASSISTANT_EN -> "영어 조교";
			case ASSISTANT_MATH -> "수학 조교";
			case STUDENT -> "학생";
			case ACADEMY_ADMIN -> "원장·관리자";
		};
	}

	private SignupInviteResponse toResponse(SignupInvitePayload payload) {
		String token = jwtService.createSignupInviteToken(payload);
		String base = signupProperties.frontendBaseUrl().replaceAll("/$", "");
		String path = buildSignupPath(payload.kind());
		String url = base + path + "?token=" + token;
		return new SignupInviteResponse(
				payload.kind(),
				payload.role(),
				roleLabel(payload.role()),
				url,
				payload.expiresAt());
	}

	private Subject defaultSubjectForRole(UserRole role) {
		return switch (role) {
			case TEACHER_KO, ASSISTANT_KO -> Subject.KO;
			case TEACHER_EN, ASSISTANT_EN -> Subject.EN;
			case TEACHER_MATH, ASSISTANT_MATH -> Subject.MATH;
			default -> null;
		};
	}
}
