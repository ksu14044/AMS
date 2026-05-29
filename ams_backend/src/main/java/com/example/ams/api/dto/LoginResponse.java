package com.example.ams.api.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResponse(TokenResponse tokens, String loginToken, List<LoginAcademyOption> academies) {

	public static LoginResponse authenticated(TokenResponse tokens) {
		return new LoginResponse(tokens, null, null);
	}

	public static LoginResponse academySelectionRequired(String loginToken, List<LoginAcademyOption> academies) {
		return new LoginResponse(null, loginToken, academies);
	}
}
