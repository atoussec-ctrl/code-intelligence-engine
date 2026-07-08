package com.rag.rag.domain.document;

import java.util.Map;
import java.util.UUID;

final class DomainValidation {

	private DomainValidation() {
	}

	static UUID requiredUuid(UUID value, String fieldName) {
		if (value == null) {
			throw new IllegalArgumentException(fieldName + " is required");
		}
		return value;
	}

	static String requiredText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " is required");
		}
		return value;
	}

	static <T> T required(T value, String fieldName) {
		if (value == null) {
			throw new IllegalArgumentException(fieldName + " is required");
		}
		return value;
	}

	static Map<String, String> metadata(Map<String, String> metadata) {
		return metadata == null ? Map.of() : Map.copyOf(metadata);
	}

}