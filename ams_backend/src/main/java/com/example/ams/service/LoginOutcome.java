package com.example.ams.service;

import java.util.List;

import com.example.ams.api.dto.LoginAcademyOption;
import com.example.ams.domain.user.User;

public sealed interface LoginOutcome permits LoginOutcome.Authenticated, LoginOutcome.AcademySelectionRequired {

	record Authenticated(User user) implements LoginOutcome {
	}

	record AcademySelectionRequired(String loginToken, List<LoginAcademyOption> academies)
			implements LoginOutcome {
	}
}
