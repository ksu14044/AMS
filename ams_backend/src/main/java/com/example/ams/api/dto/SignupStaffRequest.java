package com.example.ams.api.dto;

import com.example.ams.domain.user.Subject;
import com.example.ams.domain.user.UserRole;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupStaffRequest(
		@NotBlank String inviteToken,
		@NotBlank @Size(max = 32) String academyCode,
		@NotBlank @Size(max = 100) String name,
		@NotBlank @Email String email,
		@NotBlank @Size(min = 8, max = 100) String password,
		@NotBlank @Pattern(regexp = "^[0-9+\\-()\\s]{8,20}$", message = "전화번호 형식이 올바르지 않습니다.") String phoneNumber,
		@AssertTrue(message = "개인정보 수집 및 이용에 동의해야 가입할 수 있습니다.") boolean personalInfoConsent,
		@NotNull UserRole role,
		Subject subject) {
}
