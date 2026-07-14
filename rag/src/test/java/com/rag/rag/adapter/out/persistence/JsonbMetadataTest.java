package com.rag.rag.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

class JsonbMetadataTest {

    @Test
    void writesNullMetadataAsEmptyJson() {
        assertThat(JsonbMetadata.write(null)).isEqualTo("{}");
    }

    @Test
    void readsNullAndBlankMetadataAsEmptyMap() {
        assertThat(JsonbMetadata.read(null)).isEmpty();
        assertThat(JsonbMetadata.read("   ")).isEmpty();
    }

    @Test
    void roundTripsStringMetadata() {
        String json = JsonbMetadata.write(Map.of(
                "section", "architecture",
                "security.injection_suspected", "true"));

        assertThat(JsonbMetadata.read(json))
                .containsEntry("section", "architecture")
                .containsEntry("security.injection_suspected", "true");
    }

    @Test
    void rejectsInvalidJsonMetadata() {
        assertThatThrownBy(() -> JsonbMetadata.read("{not-json"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("metadata JSON could not be read");
    }
}
