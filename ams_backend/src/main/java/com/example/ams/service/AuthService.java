package com.example.ams.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.example.ams.api.dto.LoginAcademyOption;
import com.example.ams.common.BusinessException;
import com.example.ams.common.ErrorCode;
import com.example.ams.domain.academy.Academy;
import com.example.ams.domain.auth.SignupInviteKind;
import com.example.ams.domain.auth.SignupInvitePayload;
import com.example.ams.domain.user.Subject;
import com.example.ams.domain.user.User;
import com.example.ams.domain.user.UserRole;
import com.example.ams.domain.user.UserStatus;
import com.example.ams.repository.AcademyRepository;
import com.example.ams.repository.UserRepository;
import com.example.ams.security.JwtPrincipal;
import com.example.ams.security.JwtService;

@Service
public class AuthService {

	private final AcademyRepository academyRepository;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final SignupInviteService signupInviteService;

	public AuthService(
			AcademyRepository academyRepository,
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			JwtService jwtService,
			SignupInviteService signupInviteService) {
		this.academyRepository = academyRepository;
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.signupInviteService = signupInviteService;
	}

	@Transactional
	public User signupAcademy(
			String inviteToken,
			String academyName,
			String academyCode,
			String adminName,
			String email,
			String password,
			String phoneNumber) {
		signupInviteService.requireKind(inviteToken, SignupInviteKind.ACADEMY);
		if (academyRepository.existsByCode(academyCode)) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "이미 사용 중인 학원 코드입니다.");
		}
		Academy academy = academyRepository.insert(academyName, academyCode.toUpperCase());
		if (userRepository.existsByAcademyIdAndEmail(academy.academyId(), email)) {
			throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
		}
		return userRepository.insert(
				academy.academyId(),
				email,
				passwordEncoder.encode(password),
				adminName,
				UserRole.ACADEMY_ADMIN,
				null,
				UserStatus.ACTIVE,
				normalizePhoneNumber(phoneNumber),
				Instant.now());
	}

	@Transactional
	public User signupStaff(
			String inviteToken,
			String academyCode,
			String name,
			String email,
			String password,
			String phoneNumber,
			UserRole role,
			Subject subject) {
		SignupInvitePayload invite = signupInviteService.requireKind(inviteToken, SignupInviteKind.STAFF);
		Academy academy = signupInviteService.requireAcademy(invite);
		assertAcademyCodeMatches(invite, academyCode);
		signupInviteService.assertStaffRoleMatches(invite, role);
		validateStaffRole(role, subject);
		assertSubjectMatchesInvite(invite, subject);
		ensureEmailAvailable(academy.academyId(), email);
		return userRepository.insert(
				academy.academyId(),
				email,
				passwordEncoder.encode(password),
				name,
				role,
				subject,
				UserStatus.PENDING,
				normalizePhoneNumber(phoneNumber),
				Instant.now());
	}

	@Transactional
	public User signupStudent(
			String inviteToken,
			String academyCode,
			String name,
			String email,
			String password,
			String phoneNumber) {
		SignupInvitePayload invite = signupInviteService.requireKind(inviteToken, SignupInviteKind.STUDENT);
		Academy academy = signupInviteService.requireAcademy(invite);
		assertAcademyCodeMatches(invite, academyCode);
		ensureEmailAvailable(academy.academyId(), email);
		return userRepository.insert(
				academy.academyId(),
				email,
				passwordEncoder.encode(password),
				name,
				UserRole.STUDENT,
				null,
				UserStatus.ACTIVE,
				normalizePhoneNumber(phoneNumber),
				Instant.now());
	}

	public LoginOutcome login(String email, String password) {
		List<User> matched = findActivePasswordMatches(email, password);
		if (matched.isEmpty()) {
			throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
		}
		if (matched.size() == 1) {
			return new LoginOutcome.Authenticated(matched.get(0));
		}
		List<LoginAcademyOption> options = new ArrayList<>(matched.size());
		List<Long> userIds = new ArrayList<>(matched.size());
		for (User user : matched) {
			userIds.add(user.userId());
			options.add(LoginAcademyOption.of(user, academyRepository));
		}
		String loginToken = jwtService.createLoginSelectToken(userIds);
		return new LoginOutcome.AcademySelectionRequired(loginToken, options);
	}

	public User completeLogin(String loginToken, long userId) {
		List<Long> allowedUserIds;
		try {
			allowedUserIds = jwtService.parseLoginSelectToken(loginToken);
		} catch (Exception ex) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
		if (!allowedUserIds.contains(userId)) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
		if (user.status() == UserStatus.SUSPENDED) {
			throw new BusinessException(ErrorCode.ACCOUNT_NOT_ACTIVE);
		}
		return user;
	}

	private List<User> findActivePasswordMatches(String email, String password) {
		List<User> candidates = userRepository.findAllByEmail(email.trim());
		List<User> matched = new ArrayList<>();
		for (User user : candidates) {
			if (user.status() == UserStatus.SUSPENDED) {
				continue;
			}
			if (passwordEncoder.matches(password, user.passwordHash())) {
				matched.add(user);
			}
		}
		return matched;
	}

	public User refresh(String refreshToken) {
		JwtPrincipal principal;
		try {
			principal = jwtService.parseRefreshToken(refreshToken);
		} catch (Exception ex) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
		User user = userRepository.findById(principal.userId())
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		if (user.status() == UserStatus.SUSPENDED) {
			throw new BusinessException(ErrorCode.ACCOUNT_NOT_ACTIVE);
		}
		return user;
	}

	public String createAccessToken(User user) {
		return jwtService.createAccessToken(user);
	}

	public String createRefreshToken(User user) {
		return jwtService.createRefreshToken(user);
	}

	private Academy findAcademyByCode(String academyCode) {
		return academyRepository.findByCode(academyCode.toUpperCase())
				.orElseThrow(() -> new BusinessException(ErrorCode.ACADEMY_CODE_NOT_FOUND));
	}

	private void ensureEmailAvailable(long academyId, String email) {
		if (userRepository.existsByAcademyIdAndEmail(academyId, email)) {
			throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
		}
	}

	private void validateStaffRole(UserRole role, Subject subject) {
		if (!role.isStaffSignupRole()) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "교직원 가입에 허용되지 않은 역할입니다.");
		}
		if (role.requiresSubject() && subject == null) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "선생님·조교는 과목을 선택해야 합니다.");
		}
		if (!role.requiresSubject() && subject != null) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "해당 역할에는 과목을 지정할 수 없습니다.");
		}
	}

	private void assertAcademyCodeMatches(SignupInvitePayload invite, String academyCode) {
		if (!invite.academyCode().equalsIgnoreCase(academyCode.trim())) {
			throw new BusinessException(ErrorCode.INVALID_SIGNUP_INVITE);
		}
	}

	private void assertSubjectMatchesInvite(SignupInvitePayload invite, Subject subject) {
		if (invite.subject() != null && invite.subject() != subject) {
			throw new BusinessException(ErrorCode.INVALID_SIGNUP_INVITE);
		}
	}

	private String normalizePhoneNumber(String phoneNumber) {
		return com.example.ams.common.PhoneNumberFormatter.format(phoneNumber);
	}
}
