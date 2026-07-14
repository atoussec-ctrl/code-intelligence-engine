package com.rag.rag.adapter.out.persistence;

import java.util.Map;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

final class JsonbMetadata {

    private static final JsonMapper JSON = JsonMapper.shared();
    private static final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() {
    };

    private JsonbMetadata() {
    }

    static String write(Map<String, String> metadata) {
        try {
            return JSON.writeValueAsString(metadata == null ? Map.of() : metadata);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("metadata must be serializable as JSON", exception);
        }
    }

    static Map<String, String> read(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return JSON.readValue(json, STRING_MAP);
        } catch (JacksonException exception) {
            throw new IllegalStateException("metadata JSON could not be read", exception);
        }
    }
}
