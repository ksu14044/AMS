package com.example.ams.domain.user;

public enum UserRole {
	ACADEMY_ADMIN,
	TEACHER_KO,
	TEACHER_EN,
	TEACHER_MATH,
	STAFF_OFFICE,
	ASSISTANT_KO,
	ASSISTANT_EN,
	ASSISTANT_MATH,
	STUDENT;

	public boolean isStaffSignupRole() {
		return this != ACADEMY_ADMIN && this != STUDENT;
	}

	public boolean requiresSubject() {
		return name().startsWith("TEACHER_") || name().startsWith("ASSISTANT_");
	}

	public boolean isHomeroomTeacher() {
		return name().startsWith("TEACHER_");
	}

	public boolean isAssistant() {
		return name().startsWith("ASSISTANT_");
	}

	public boolean isOfficeStaff() {
		return this == STAFF_OFFICE;
	}
}
