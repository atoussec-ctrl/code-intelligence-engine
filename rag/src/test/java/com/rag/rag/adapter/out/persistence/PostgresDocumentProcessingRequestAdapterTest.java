package com.rag.rag.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class PostgresDocumentProcessingRequestAdapterTest {

	@Test
	void validatesDependencyAndApplicationPortArguments() {
		assertEquals(
			"jdbcTemplate is required",
			assertThrows(
				NullPointerException.class,
				() -> new PostgresDocumentProcessingRequestAdapter(null)).getMessage());

		var adapter = new PostgresDocumentProcessingRequestAdapter(mock(JdbcTemplate.class));
		assertEquals(
			"command is required",
			assertThrows(NullPointerException.class, () -> adapter.create(null)).getMessage());
		assertEquals(
			"request id is required",
			assertThrows(
				NullPointerException.class,
				() -> adapter.claimForProcessing(null, Duration.ofMinutes(1))).getMessage());
		assertEquals(
			"processing lease duration must be positive",
			assertThrows(
				IllegalArgumentException.class,
				() -> adapter.claimForProcessing(UUID.randomUUID(), Duration.ZERO)).getMessage());
		assertEquals(
			"workspace id is required",
			assertThrows(
				NullPointerException.class,
				() -> adapter.findById(null, UUID.randomUUID(), UUID.randomUUID())).getMessage());
		assertEquals(
			"document id is required",
			assertThrows(
				NullPointerException.class,
				() -> adapter.findById(UUID.randomUUID(), null, UUID.randomUUID())).getMessage());
		assertEquals(
			"request id is required",
			assertThrows(
				NullPointerException.class,
				() -> adapter.findById(UUID.randomUUID(), UUID.randomUUID(), null)).getMessage());
		assertEquals(
			"request id is required",
			assertThrows(NullPointerException.class, () -> adapter.markCompleted(null)).getMessage());
		assertEquals(
			"request id is required",
			assertThrows(NullPointerException.class, () -> adapter.releaseForRetry(null, "failure")).getMessage());
		assertEquals(
			"request id is required",
			assertThrows(NullPointerException.class, () -> adapter.markFailed(null, "failure")).getMessage());
	}

	@Test
	void validatesOutboxArguments() {
		var adapter = new PostgresDocumentProcessingRequestAdapter(mock(JdbcTemplate.class));

		assertEquals(
			"batch size must be positive",
			assertThrows(
				IllegalArgumentException.class,
				() -> adapter.claimPending(0, Duration.ofSeconds(1))).getMessage());
		assertEquals(
			"lease duration must be positive",
			assertThrows(
				IllegalArgumentException.class,
				() -> adapter.claimPending(1, Duration.ZERO)).getMessage());
		assertEquals(
			"lease duration must be positive",
			assertThrows(
				NullPointerException.class,
				() -> adapter.claimPending(1, null)).getMessage());
		assertEquals(
			"request id is required",
			assertThrows(NullPointerException.class, () -> adapter.markPublished(null)).getMessage());
		assertEquals(
			"request id is required",
			assertThrows(
				NullPointerException.class,
				() -> adapter.reschedule(null, "failure", Duration.ofSeconds(1))).getMessage());
		assertEquals(
			"retry delay must be positive",
			assertThrows(
				IllegalArgumentException.class,
				() -> adapter.reschedule(UUID.randomUUID(), "failure", Duration.ZERO)).getMessage());
	}

}
