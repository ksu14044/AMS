package com.example.ams.api.dto;

import com.example.ams.domain.user.User;
import com.example.ams.domain.user.UserRole;
import com.example.ams.repository.AcademyRepository;

public record LoginAcademyOption(long userId, String academyName, String name, UserRole role) {

	public static LoginAcademyOption of(User user, String academyName) {
		return new LoginAcademyOption(user.userId(), academyName, user.name(), user.role());
	}

	public static LoginAcademyOption of(User user, AcademyRepository academyRepository) {
		String academyName = academyRepository.findById(user.academyId())
				.map(a -> a.name())
				.orElse("학원");
		return of(user, academyName);
	}
}
