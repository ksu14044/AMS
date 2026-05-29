package com.example.ams.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupAcademyRequest(
		@NotBlank String inviteToken,
		@NotBlank @Size(max = 100) String academyName,
		@NotBlank @Size(min = 4, max = 32) @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "학원 코드는 영문, 숫자, _, - 만 사용할 수 있습니다.") String academyCode,
		@NotBlank @Size(max = 100) String adminName,
		@NotBlank @Email String email,
		@NotBlank @Size(min = 8, max = 100) String password,
		@NotBlank @Pattern(regexp = "^[0-9+\\-()\\s]{8,20}$", message = "전화번호 형식이 올바르지 않습니다.") String phoneNumber,
		@AssertTrue(message = "개인정보 수집 및 이용에 동의해야 가입할 수 있습니다.") boolean personalInfoConsent) {
}
