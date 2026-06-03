package com.example.ams.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class WrongQuestionNosJson {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private WrongQuestionNosJson() {
	}

	public static String toJson(List<Integer> nos) {
		if (nos == null || nos.isEmpty()) {
			return "[]";
		}
		try {
			return MAPPER.writeValueAsString(nos);
		} catch (Exception ex) {
			throw new IllegalStateException("wrong_question_nos JSON 직렬화 실패", ex);
		}
	}

	public static List<Integer> fromJson(String json) {
		if (json == null || json.isBlank()) {
			return List.of();
		}
		try {
			List<Integer> parsed = MAPPER.readValue(json, new TypeReference<>() {
			});
			if (parsed == null) {
				return List.of();
			}
			return List.copyOf(parsed);
		} catch (Exception ex) {
			return List.of();
		}
	}

	public static List<Integer> normalize(List<Integer> nos, int questionCount) {
		if (nos == null || nos.isEmpty()) {
			return List.of();
		}
		List<Integer> sorted = new ArrayList<>();
		for (Integer no : nos) {
			if (no == null || no < 1 || no > questionCount) {
				continue;
			}
			if (!sorted.contains(no)) {
				sorted.add(no);
			}
		}
		Collections.sort(sorted);
		return List.copyOf(sorted);
	}
}
