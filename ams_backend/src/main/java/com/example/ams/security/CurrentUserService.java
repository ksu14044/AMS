package com.example.ams.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.example.ams.common.BusinessException;
import com.example.ams.common.ErrorCode;
import com.example.ams.domain.user.UserRole;

@Service
public class CurrentUserService {

	public AuthenticatedUser requireAuthenticated() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
		return user;
	}

	public JwtPrincipal requirePrincipal() {
		return requireAuthenticated().principal();
	}

	public long requireAcademyId() {
		return requirePrincipal().academyId();
	}

	public long requireUserId() {
		return requirePrincipal().userId();
	}

	public UserRole requireRole() {
		return requirePrincipal().role();
	}

	public void requireAcademyAdmin() {
		if (!requireAuthenticated().isAcademyAdmin()) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
	}

	public void assertSameAcademy(long resourceAcademyId) {
		if (requireAcademyId() != resourceAcademyId) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
	}
}
