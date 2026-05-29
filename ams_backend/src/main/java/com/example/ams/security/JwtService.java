package com.example.ams.security;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.example.ams.config.JwtProperties;
import com.example.ams.domain.auth.SignupInviteKind;
import com.example.ams.domain.auth.SignupInvitePayload;
import com.example.ams.domain.user.Subject;
import com.example.ams.domain.user.User;
import com.example.ams.domain.user.UserRole;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

	private static final String CLAIM_ACADEMY_ID = "academyId";
	private static final String CLAIM_ROLE = "role";
	private static final String CLAIM_STATUS = "status";
	private static final String CLAIM_TYPE = "type";
	private static final String TYPE_ACCESS = "access";
	private static final String TYPE_REFRESH = "refresh";
	private static final String TYPE_LOGIN_SELECT = "login_select";
	private static final String TYPE_SIGNUP_INVITE = "signup_invite";
	private static final String CLAIM_USER_IDS = "userIds";
	private static final String CLAIM_SIGNUP_KIND = "signupKind";
	private static final String CLAIM_ACADEMY_CODE = "academyCode";
	private static final String CLAIM_SUBJECT = "subject";

	private final JwtProperties properties;
	private final SecretKey secretKey;

	public JwtService(JwtProperties properties) {
		this.properties = properties;
		this.secretKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
	}

	public String createAccessToken(User user) {
		return buildToken(user, TYPE_ACCESS, properties.accessExpirationMs());
	}

	public String createRefreshToken(User user) {
		return buildToken(user, TYPE_REFRESH, properties.refreshExpirationMs());
	}

	public JwtPrincipal parseAccessToken(String token) {
		Claims claims = parseClaims(token);
		if (!TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
			throw new IllegalArgumentException("Not an access token");
		}
		return toPrincipal(claims);
	}

	public JwtPrincipal parseRefreshToken(String token) {
		Claims claims = parseClaims(token);
		if (!TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class))) {
			throw new IllegalArgumentException("Not a refresh token");
		}
		return toPrincipal(claims);
	}

	public String createLoginSelectToken(List<Long> userIds) {
		Date now = new Date();
		Date expiry = new Date(now.getTime() + properties.loginSelectExpirationMs());
		return Jwts.builder()
				.subject("login-select")
				.claim(CLAIM_USER_IDS, userIds)
				.claim(CLAIM_TYPE, TYPE_LOGIN_SELECT)
				.issuedAt(now)
				.expiration(expiry)
				.signWith(secretKey)
				.compact();
	}

	public String createSignupInviteToken(SignupInvitePayload payload) {
		Date now = new Date();
		Date expiry = Date.from(payload.expiresAt());
		var builder = Jwts.builder()
				.subject("signup-invite")
				.claim(CLAIM_TYPE, TYPE_SIGNUP_INVITE)
				.claim(CLAIM_SIGNUP_KIND, payload.kind().name())
				.issuedAt(now)
				.expiration(expiry);
		if (payload.academyId() != null) {
			builder.claim(CLAIM_ACADEMY_ID, payload.academyId());
		}
		if (payload.academyCode() != null) {
			builder.claim(CLAIM_ACADEMY_CODE, payload.academyCode());
		}
		if (payload.role() != null) {
			builder.claim(CLAIM_ROLE, payload.role().name());
		}
		if (payload.subject() != null) {
			builder.claim(CLAIM_SUBJECT, payload.subject().name());
		}
		return builder.signWith(secretKey).compact();
	}

	public SignupInvitePayload parseSignupInviteToken(String token) {
		Claims claims = parseClaims(token);
		if (!TYPE_SIGNUP_INVITE.equals(claims.get(CLAIM_TYPE, String.class))) {
			throw new IllegalArgumentException("Not a signup invite token");
		}
		SignupInviteKind kind = SignupInviteKind.valueOf(claims.get(CLAIM_SIGNUP_KIND, String.class));
		Long academyId = claims.get(CLAIM_ACADEMY_ID, Long.class);
		String academyCode = claims.get(CLAIM_ACADEMY_CODE, String.class);
		UserRole role = claims.get(CLAIM_ROLE, String.class) != null
				? UserRole.valueOf(claims.get(CLAIM_ROLE, String.class))
				: null;
		Subject subject = claims.get(CLAIM_SUBJECT, String.class) != null
				? Subject.valueOf(claims.get(CLAIM_SUBJECT, String.class))
				: null;
		return new SignupInvitePayload(kind, academyId, academyCode, role, subject, claims.getExpiration().toInstant());
	}

	public List<Long> parseLoginSelectToken(String token) {
		Claims claims = parseClaims(token);
		if (!TYPE_LOGIN_SELECT.equals(claims.get(CLAIM_TYPE, String.class))) {
			throw new IllegalArgumentException("Not a login select token");
		}
		Object raw = claims.get(CLAIM_USER_IDS);
		if (!(raw instanceof List<?> list)) {
			throw new IllegalArgumentException("Missing userIds claim");
		}
		List<Long> userIds = new ArrayList<>(list.size());
		for (Object item : list) {
			if (item instanceof Number number) {
				userIds.add(number.longValue());
			} else {
				throw new IllegalArgumentException("Invalid userId in login select token");
			}
		}
		return List.copyOf(userIds);
	}

	private Claims parseClaims(String token) {
		return Jwts.parser()
				.verifyWith(secretKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

	private String buildToken(User user, String type, long expirationMs) {
		Date now = new Date();
		Date expiry = new Date(now.getTime() + expirationMs);
		return Jwts.builder()
				.subject(String.valueOf(user.userId()))
				.claim(CLAIM_ACADEMY_ID, user.academyId())
				.claim(CLAIM_ROLE, user.role().name())
				.claim(CLAIM_STATUS, user.status().name())
				.claim(CLAIM_TYPE, type)
				.issuedAt(now)
				.expiration(expiry)
				.signWith(secretKey)
				.compact();
	}

	private JwtPrincipal toPrincipal(Claims claims) {
		return new JwtPrincipal(
				Long.parseLong(claims.getSubject()),
				claims.get(CLAIM_ACADEMY_ID, Long.class),
				UserRole.valueOf(claims.get(CLAIM_ROLE, String.class)),
				claims.get(CLAIM_STATUS, String.class));
	}
}
