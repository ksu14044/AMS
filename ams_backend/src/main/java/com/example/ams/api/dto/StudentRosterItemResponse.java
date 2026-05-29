package com.example.ams.api.dto;

import java.time.Instant;
import java.util.List;

import com.example.ams.common.PhoneNumberFormatter;
import com.example.ams.domain.user.StudentRosterRow;
import com.example.ams.domain.user.UserStatus;

public record StudentRosterItemResponse(
		long userId,
		String name,
		String email,
		String phoneNumber,
		UserStatus status,
		Instant createdAt,
		List<EnrolledClassResponse> classes) {

	public record EnrolledClassResponse(long classId, String name, String subject) {
		public static EnrolledClassResponse from(StudentRosterRow.EnrolledClass row) {
			return new EnrolledClassResponse(row.classId(), row.name(), row.subject());
		}
	}

	public static StudentRosterItemResponse from(StudentRosterRow row) {
		return new StudentRosterItemResponse(
				row.userId(),
				row.name(),
				row.email(),
				PhoneNumberFormatter.format(row.phoneNumber()),
				row.status(),
				row.createdAt(),
				row.classes().stream().map(EnrolledClassResponse::from).toList());
	}
}
