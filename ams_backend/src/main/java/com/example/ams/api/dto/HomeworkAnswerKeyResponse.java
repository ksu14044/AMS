package com.example.ams.api.dto;

import java.util.List;

import com.example.ams.domain.clazz.Homework;
import com.example.ams.domain.clazz.HomeworkAnswerKey;

public record HomeworkAnswerKeyResponse(int questionCount, List<HomeworkAnswerKeyItemResponse> items) {

	public static HomeworkAnswerKeyResponse from(Homework homework, List<HomeworkAnswerKey> keys) {
		int count = homework.questionCount() != null ? homework.questionCount() : keys.size();
		List<HomeworkAnswerKeyItemResponse> items = keys.stream()
				.map(k -> new HomeworkAnswerKeyItemResponse(k.questionNo(), k.correctAnswer()))
				.toList();
		return new HomeworkAnswerKeyResponse(count, items);
	}
}
