package com.example.ams.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
	INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
	INVALID_SIGNUP_INVITE(HttpStatus.BAD_REQUEST, "유효하지 않거나 만료된 가입 링크입니다."),
	ACADEMY_CODE_NOT_FOUND(HttpStatus.NOT_FOUND, "학원 코드를 찾을 수 없습니다."),
	EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
	INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
	USER_NOT_PENDING(HttpStatus.BAD_REQUEST, "승인 대기 상태의 사용자가 아닙니다."),
	ACCOUNT_NOT_ACTIVE(HttpStatus.FORBIDDEN, "승인되지 않았거나 비활성 계정입니다."),
	CLASS_NOT_FOUND(HttpStatus.NOT_FOUND, "반을 찾을 수 없습니다."),
	LESSON_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "수업기록을 찾을 수 없습니다."),
	LESSON_RECORD_DATE_EXISTS(HttpStatus.CONFLICT, "해당 날짜의 수업기록이 이미 있습니다."),
	LESSON_RECORD_LINK_NOT_FOUND(HttpStatus.NOT_FOUND, "수업기록에 연결된 항목이 아닙니다."),
	LESSON_RECORD_LINK_LOCKED(HttpStatus.CONFLICT, "진행 중인 항목은 삭제할 수 없습니다."),
	CLASS_NAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 반 이름입니다."),
	INVALID_HOMEROOM_TEACHER(HttpStatus.BAD_REQUEST, "담임으로 지정할 수 없는 선생님입니다."),
	ALREADY_ENROLLED(HttpStatus.CONFLICT, "이미 해당 반에 배정된 학생입니다."),
	ENROLLMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "수강 배정을 찾을 수 없습니다."),
	HOMEWORK_NOT_FOUND(HttpStatus.NOT_FOUND, "숙제를 찾을 수 없습니다."),
	TEST_NOT_FOUND(HttpStatus.NOT_FOUND, "테스트를 찾을 수 없습니다."),
	REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "보고서를 찾을 수 없습니다."),
	VIDEO_NOT_FOUND(HttpStatus.NOT_FOUND, "영상을 찾을 수 없습니다."),
	CLINIC_SLOT_NOT_FOUND(HttpStatus.NOT_FOUND, "클리닉 슬롯을 찾을 수 없습니다."),
	CLINIC_WEEK_LOCKED(HttpStatus.BAD_REQUEST, "클리닉 예약 기간이 종료되었습니다."),
	CLINIC_SLOT_FULL(HttpStatus.CONFLICT, "클리닉 슬롯 정원이 찼습니다."),
	CLINIC_TIME_CONFLICT(HttpStatus.CONFLICT, "같은 요일·시간에 이미 클리닉을 예약했습니다."),
	RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "예약을 찾을 수 없습니다."),
	NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."),
	FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

	private final HttpStatus httpStatus;
	private final String message;

	ErrorCode(HttpStatus httpStatus, String message) {
		this.httpStatus = httpStatus;
		this.message = message;
	}

	public HttpStatus httpStatus() {
		return httpStatus;
	}

	public String message() {
		return message;
	}
}
