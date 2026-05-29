package com.example.ams.security;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * /api/v1/admin/** 요청에 대해 학원 관리자 역할을 검증한다 (테넌트 API 1차 게이트).
 */
@Component
public class AcademyAdminInterceptor implements HandlerInterceptor {

	private final CurrentUserService currentUserService;

	public AcademyAdminInterceptor(CurrentUserService currentUserService) {
		this.currentUserService = currentUserService;
	}

	@Override
	public boolean preHandle(
			@NonNull HttpServletRequest request,
			@NonNull HttpServletResponse response,
			@NonNull Object handler) {
		currentUserService.requireAcademyAdmin();
		return true;
	}
}
