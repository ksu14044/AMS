package com.example.ams.common;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.example.ams.domain.clazz.ClinicResultFieldDef;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class ClinicResultSchemaJson {

	public static final String DEFAULT_SCHEMA_JSON = """
			{"fields":[
			{"key":"attended","label":"참석","type":"boolean","required":true},
			{"key":"memo","label":"메모","type":"text","maxLength":500}
			]}""";

	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final Pattern KEY_PATTERN = Pattern.compile("^[a-z][a-z0-9_]{0,31}$");
	private static final Set<String> ALLOWED_TYPES = Set.of("boolean", "text", "number", "select");
	private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
	};

	private ClinicResultSchemaJson() {
	}

	public static List<ClinicResultFieldDef> parseFields(String schemaJson) {
		if (schemaJson == null || schemaJson.isBlank()) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "프리셋 필드 정의가 비어 있습니다.");
		}
		try {
			JsonNode root = MAPPER.readTree(schemaJson);
			JsonNode fieldsNode = root.get("fields");
			if (fieldsNode == null || !fieldsNode.isArray() || fieldsNode.isEmpty()) {
				throw invalidSchema("필드는 1개 이상 필요합니다.");
			}
			List<ClinicResultFieldDef> fields = new ArrayList<>();
			for (JsonNode node : fieldsNode) {
				fields.add(parseField(node));
			}
			validateUniqueKeys(fields);
			return List.copyOf(fields);
		} catch (BusinessException ex) {
			throw ex;
		} catch (Exception ex) {
			throw invalidSchema("프리셋 JSON 형식이 올바르지 않습니다.");
		}
	}

	public static String toJson(List<ClinicResultFieldDef> fields) {
		validateUniqueKeys(fields);
		for (ClinicResultFieldDef field : fields) {
			validateField(field);
		}
		try {
			return MAPPER.writeValueAsString(Map.of("fields", fields));
		} catch (JsonProcessingException ex) {
			throw invalidSchema("프리셋 JSON을 저장할 수 없습니다.");
		}
	}

	public static Map<String, Object> validateAndNormalizeValues(
			List<ClinicResultFieldDef> fields,
			Map<String, Object> rawValues) {
		Map<String, Object> normalized = new LinkedHashMap<>();
		for (ClinicResultFieldDef field : fields) {
			Object raw = rawValues != null ? rawValues.get(field.key()) : null;
			Object value = normalizeValue(field, raw);
			if (Boolean.TRUE.equals(field.required()) && isEmpty(value)) {
				throw new BusinessException(ErrorCode.INVALID_REQUEST, field.label() + "은(는) 필수입니다.");
			}
			if (!isEmpty(value)) {
				normalized.put(field.key(), value);
			}
		}
		return normalized;
	}

	public static boolean isComplete(List<ClinicResultFieldDef> fields, String resultJson) {
		if (resultJson == null || resultJson.isBlank()) {
			return false;
		}
		Map<String, Object> values = fromValuesJson(resultJson);
		for (ClinicResultFieldDef field : fields) {
			if (!Boolean.TRUE.equals(field.required())) {
				continue;
			}
			if (isEmpty(values.get(field.key()))) {
				return false;
			}
		}
		return !fields.stream().filter(f -> Boolean.TRUE.equals(f.required())).toList().isEmpty()
				|| !values.isEmpty();
	}

	public static Boolean attendedValue(String resultJson) {
		if (resultJson == null || resultJson.isBlank()) {
			return null;
		}
		Object attended = fromValuesJson(resultJson).get("attended");
		if (attended == null) {
			return null;
		}
		if (attended instanceof Boolean bool) {
			return bool;
		}
		if (attended instanceof String text) {
			if ("true".equalsIgnoreCase(text)) {
				return true;
			}
			if ("false".equalsIgnoreCase(text)) {
				return false;
			}
		}
		return null;
	}

	public static String memoValue(String resultJson) {
		if (resultJson == null || resultJson.isBlank()) {
			return null;
		}
		Object memo = fromValuesJson(resultJson).get("memo");
		if (memo == null) {
			return null;
		}
		String text = String.valueOf(memo).trim();
		return text.isEmpty() ? null : text;
	}

	public static String toValuesJson(Map<String, Object> values) {
		if (values == null || values.isEmpty()) {
			return null;
		}
		try {
			return MAPPER.writeValueAsString(values);
		} catch (JsonProcessingException ex) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "결과 JSON을 저장할 수 없습니다.");
		}
	}

	public static Map<String, Object> fromValuesJson(String json) {
		if (json == null || json.isBlank()) {
			return Map.of();
		}
		try {
			return MAPPER.readValue(json, MAP_TYPE);
		} catch (JsonProcessingException ex) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "저장된 결과를 읽을 수 없습니다.");
		}
	}

	private static ClinicResultFieldDef parseField(JsonNode node) {
		String key = text(node, "key");
		String label = text(node, "label");
		String type = text(node, "type");
		Boolean required = node.has("required") && !node.get("required").isNull()
				? node.get("required").asBoolean()
				: null;
		Integer maxLength = node.has("maxLength") && !node.get("maxLength").isNull()
				? node.get("maxLength").asInt()
				: null;
		List<String> options = null;
		if (node.has("options") && node.get("options").isArray()) {
			options = new ArrayList<>();
			for (JsonNode option : node.get("options")) {
				options.add(option.asText());
			}
		}
		return new ClinicResultFieldDef(key, label, type, required, maxLength, options);
	}

	private static void validateUniqueKeys(List<ClinicResultFieldDef> fields) {
		var keys = fields.stream().map(ClinicResultFieldDef::key).toList();
		if (keys.size() != Set.copyOf(keys).size()) {
			throw invalidSchema("필드 key는 중복될 수 없습니다.");
		}
	}

	private static void validateField(ClinicResultFieldDef field) {
		if (field.key() == null || !KEY_PATTERN.matcher(field.key()).matches()) {
			throw invalidSchema("필드 key는 영문 소문자로 시작해야 합니다.");
		}
		if (field.label() == null || field.label().isBlank()) {
			throw invalidSchema("필드 label은 필수입니다.");
		}
		if (field.type() == null || !ALLOWED_TYPES.contains(field.type())) {
			throw invalidSchema("지원하지 않는 필드 type입니다.");
		}
		if ("select".equals(field.type())) {
			if (field.options() == null || field.options().isEmpty()) {
				throw invalidSchema("select 필드는 options가 필요합니다.");
			}
		}
		if ("text".equals(field.type()) && field.maxLength() != null && field.maxLength() < 1) {
			throw invalidSchema("text maxLength는 1 이상이어야 합니다.");
		}
	}

	private static Object normalizeValue(ClinicResultFieldDef field, Object raw) {
		if (raw == null) {
			return null;
		}
		return switch (field.type()) {
			case "boolean" -> normalizeBoolean(raw, field.label());
			case "text" -> normalizeText(raw, field);
			case "number" -> normalizeNumber(raw, field.label());
			case "select" -> normalizeSelect(raw, field);
			default -> throw invalidSchema("지원하지 않는 필드 type입니다.");
		};
	}

	private static Boolean normalizeBoolean(Object raw, String label) {
		if (raw instanceof Boolean bool) {
			return bool;
		}
		if (raw instanceof String text) {
			if (text.isBlank()) {
				return null;
			}
			if ("true".equalsIgnoreCase(text)) {
				return true;
			}
			if ("false".equalsIgnoreCase(text)) {
				return false;
			}
		}
		throw new BusinessException(ErrorCode.INVALID_REQUEST, label + " 값이 올바르지 않습니다.");
	}

	private static String normalizeText(Object raw, ClinicResultFieldDef field) {
		String text = String.valueOf(raw).trim();
		if (text.isEmpty()) {
			return null;
		}
		if (field.maxLength() != null && text.length() > field.maxLength()) {
			throw new BusinessException(
					ErrorCode.INVALID_REQUEST,
					field.label() + "은(는) " + field.maxLength() + "자 이하여야 합니다.");
		}
		return text;
	}

	private static Number normalizeNumber(Object raw, String label) {
		if (raw instanceof Number number) {
			return number;
		}
		if (raw instanceof String text && !text.isBlank()) {
			try {
				if (text.contains(".")) {
					return Double.parseDouble(text);
				}
				return Long.parseLong(text);
			} catch (NumberFormatException ex) {
				throw new BusinessException(ErrorCode.INVALID_REQUEST, label + " 값이 올바르지 않습니다.");
			}
		}
		throw new BusinessException(ErrorCode.INVALID_REQUEST, label + " 값이 올바르지 않습니다.");
	}

	private static String normalizeSelect(Object raw, ClinicResultFieldDef field) {
		String text = String.valueOf(raw).trim();
		if (text.isEmpty()) {
			return null;
		}
		if (field.options() == null || !field.options().contains(text)) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, field.label() + " 선택값이 올바르지 않습니다.");
		}
		return text;
	}

	private static boolean isEmpty(Object value) {
		if (value == null) {
			return true;
		}
		if (value instanceof String text) {
			return text.isBlank();
		}
		return false;
	}

	private static String text(JsonNode node, String field) {
		JsonNode value = node.get(field);
		if (value == null || value.isNull()) {
			return null;
		}
		return value.asText();
	}

	private static BusinessException invalidSchema(String message) {
		return new BusinessException(ErrorCode.INVALID_REQUEST, message);
	}
}
