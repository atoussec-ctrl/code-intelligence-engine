package com.rag.rag.adapter.messaging;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.rag.rag.adapter.out.messaging.RabbitDocumentProcessingPublisher;
import com.rag.rag.application.usecase.ProcessQueuedDocumentUseCase;
import com.rag.rag.config.RabbitDocumentProcessingConfig;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(properties = {
	"spring.rabbitmq.listener.simple.auto-startup=true",
	"spring.rabbitmq.listener.simple.retry.initial-interval=10ms",
	"spring.rabbitmq.listener.simple.retry.multiplier=1",
	"spring.rabbitmq.listener.simple.retry.max-interval=10ms",
	"spring.rabbitmq.publisher-confirm-type=correlated",
	"spring.rabbitmq.publisher-returns=true",
	"spring.rabbitmq.template.mandatory=true"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers(disabledWithoutDocker = true)
class RabbitDocumentProcessingIntegrationTest {

	@Container
	private static final RabbitMQContainer RABBITMQ =
		new RabbitMQContainer(DockerImageName.parse("rabbitmq:3-management"));

	@Autowired
	private RabbitDocumentProcessingPublisher publisher;

	@Autowired
	private RabbitAdmin rabbitAdmin;

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@MockitoBean
	private ProcessQueuedDocumentUseCase processDocument;

	@DynamicPropertySource
	static void rabbitProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
		registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
		registry.add("spring.rabbitmq.username", RABBITMQ::getAdminUsername);
		registry.add("spring.rabbitmq.password", RABBITMQ::getAdminPassword);
	}

	@BeforeEach
	void purgeQueues() {
		rabbitAdmin.purgeQueue(RabbitDocumentProcessingConfig.PROCESSING_QUEUE, false);
		rabbitAdmin.purgeQueue(RabbitDocumentProcessingConfig.DEAD_LETTER_QUEUE, false);
	}

	@Test
	void publishesAndConsumesDocumentProcessingMessage() {
		var requestId = UUID.randomUUID();

		publisher.publish(requestId);

		verify(processDocument, timeout(Duration.ofSeconds(10).toMillis())).execute(requestId);
	}

	@Test
	void retriesFailedProcessingAndRoutesExhaustedMessageToDeadLetterQueue() {
		var requestId = UUID.randomUUID();
		doThrow(new IllegalStateException("embedding provider unavailable"))
			.when(processDocument).execute(requestId);

		publisher.publish(requestId);

		verify(processDocument, timeout(Duration.ofSeconds(10).toMillis()).times(3)).execute(requestId);
		var failedMessage = new AtomicReference<Object>();
		await().atMost(Duration.ofSeconds(10)).until(() -> {
			var received = rabbitTemplate.receiveAndConvert(RabbitDocumentProcessingConfig.DEAD_LETTER_QUEUE);
			if (received == null) {
				return false;
			}
			failedMessage.set(received);
			return true;
		});
		assertEquals(new DocumentProcessingMessage(requestId), failedMessage.get());
	}

}
