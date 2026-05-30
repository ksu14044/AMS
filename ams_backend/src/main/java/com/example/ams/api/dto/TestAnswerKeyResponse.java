package com.example.ams.api.dto;

import java.util.List;

import com.example.ams.domain.clazz.TestAnswerKey;
import com.example.ams.domain.clazz.TestExam;

public record TestAnswerKeyResponse(int questionCount, List<HomeworkAnswerKeyItemResponse> items) {

	public static TestAnswerKeyResponse from(TestExam test, List<TestAnswerKey> keys) {
		int count = test.questionCount() != null ? test.questionCount() : keys.size();
		List<HomeworkAnswerKeyItemResponse> items = keys.stream()
				.map(k -> new HomeworkAnswerKeyItemResponse(k.questionNo(), k.correctAnswer()))
				.toList();
		return new TestAnswerKeyResponse(count, items);
	}
}
