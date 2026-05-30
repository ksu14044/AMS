package com.example.ams.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class HomeworkAnswersJson {

	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final TypeReference<List<String>> LIST_TYPE = new TypeReference<>() {
	};

	private HomeworkAnswersJson() {
	}

	public static String toJson(List<String> answers) {
		if (answers == null || answers.isEmpty()) {
			return null;
		}
		try {
			return MAPPER.writeValueAsString(normalizeSize(answers));
		} catch (JsonProcessingException ex) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "답안 형식이 올바르지 않습니다.");
		}
	}

	public static List<String> fromJson(String json, int questionCount) {
		if (json == null || json.isBlank()) {
			return emptyAnswers(questionCount);
		}
		try {
			List<String> parsed = MAPPER.readValue(json, LIST_TYPE);
			return normalizeToCount(parsed, questionCount);
		} catch (JsonProcessingException ex) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "저장된 답안을 읽을 수 없습니다.");
		}
	}

	public static List<String> normalizeToCount(List<String> answers, int questionCount) {
		List<String> normalized = new ArrayList<>(Collections.nCopies(questionCount, ""));
		if (answers == null) {
			return normalized;
		}
		for (int i = 0; i < questionCount; i++) {
			if (i < answers.size() && answers.get(i) != null) {
				normalized.set(i, answers.get(i));
			}
		}
		return normalized;
	}

	private static List<String> normalizeSize(List<String> answers) {
		List<String> normalized = new ArrayList<>();
		for (String answer : answers) {
			normalized.add(answer != null ? answer : "");
		}
		return normalized;
	}

	private static List<String> emptyAnswers(int questionCount) {
		return new ArrayList<>(Collections.nCopies(Math.max(questionCount, 0), ""));
	}
}
