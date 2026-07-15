package com.rag.rag.adapter.out.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.rag.rag.adapter.out.persistence.ClaimedDocumentProcessingRequest;
import com.rag.rag.adapter.out.persistence.PostgresDocumentProcessingRequestAdapter;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DocumentProcessingOutboxDispatcherTest {

	@Test
	void confirmsPublishedRequestsAndReschedulesFailuresWithBoundedBackoff() {
		var requests = mock(PostgresDocumentProcessingRequestAdapter.class);
		var publisher = mock(RabbitDocumentProcessingPublisher.class);
		var publishedId = UUID.randomUUID();
		var failedId = UUID.randomUUID();
		var lateFailureId = UUID.randomUUID();
		var lease = Duration.ofSeconds(30);
		when(requests.claimPending(20, lease)).thenReturn(List.of(
			new ClaimedDocumentProcessingRequest(publishedId, 1),
			new ClaimedDocumentProcessingRequest(failedId, 1),
			new ClaimedDocumentProcessingRequest(lateFailureId, 100)));
		doThrow(new IllegalStateException("connection refused")).when(publisher).publish(failedId);
		doThrow(new IllegalStateException()).when(publisher).publish(lateFailureId);
		var dispatcher = new DocumentProcessingOutboxDispatcher(requests, publisher, 20, lease);

		dispatcher.dispatch();

		verify(requests).markPublished(publishedId);
		verify(requests).reschedule(failedId, "connection refused", Duration.ofSeconds(1));
		verify(requests).reschedule(lateFailureId, "IllegalStateException", Duration.ofSeconds(64));
	}

	@Test
	void doesNothingWhenNoRequestCanBeClaimed() {
		var requests = mock(PostgresDocumentProcessingRequestAdapter.class);
		var publisher = mock(RabbitDocumentProcessingPublisher.class);
		var lease = Duration.ofSeconds(30);
		when(requests.claimPending(20, lease)).thenReturn(List.of());
		var dispatcher = new DocumentProcessingOutboxDispatcher(requests, publisher, 20, lease);

		dispatcher.dispatch();

		verifyNoInteractions(publisher);
	}

	@Test
	void validatesDependenciesAndSettings() {
		var requests = mock(PostgresDocumentProcessingRequestAdapter.class);
		var publisher = mock(RabbitDocumentProcessingPublisher.class);

		assertEquals(
			"processing requests are required",
			assertThrows(
				NullPointerException.class,
				() -> new DocumentProcessingOutboxDispatcher(null, publisher, 20, Duration.ofSeconds(30)))
				.getMessage());
		assertEquals(
			"publisher is required",
			assertThrows(
				NullPointerException.class,
				() -> new DocumentProcessingOutboxDispatcher(requests, null, 20, Duration.ofSeconds(30)))
				.getMessage());
		assertEquals(
			"batch size must be positive",
			assertThrows(
				IllegalArgumentException.class,
				() -> new DocumentProcessingOutboxDispatcher(requests, publisher, 0, Duration.ofSeconds(30)))
				.getMessage());
		assertEquals(
			"lease duration is required",
			assertThrows(
				NullPointerException.class,
				() -> new DocumentProcessingOutboxDispatcher(requests, publisher, 20, null))
				.getMessage());
		assertEquals(
			"lease duration must be positive",
			assertThrows(
				IllegalArgumentException.class,
				() -> new DocumentProcessingOutboxDispatcher(requests, publisher, 20, Duration.ZERO))
				.getMessage());
	}

}
