package com.rag.rag.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;

class RabbitDocumentProcessingConfigTest {

	private final RabbitDocumentProcessingConfig config = new RabbitDocumentProcessingConfig();

	@Test
	void declaresDurableProcessingTopologyWithDeadLetterRouting() {
		var exchange = config.documentProcessingExchange();
		var queue = config.documentProcessingQueue();
		var binding = config.documentProcessingBinding(queue, exchange);

		assertEquals(RabbitDocumentProcessingConfig.PROCESSING_EXCHANGE, exchange.getName());
		assertTrue(exchange.isDurable());
		assertFalse(exchange.isAutoDelete());
		assertEquals(RabbitDocumentProcessingConfig.PROCESSING_QUEUE, queue.getName());
		assertTrue(queue.isDurable());
		assertEquals(
			RabbitDocumentProcessingConfig.DEAD_LETTER_EXCHANGE,
			queue.getArguments().get("x-dead-letter-exchange"));
		assertEquals(
			RabbitDocumentProcessingConfig.DEAD_LETTER_ROUTING_KEY,
			queue.getArguments().get("x-dead-letter-routing-key"));
		assertEquals(RabbitDocumentProcessingConfig.PROCESSING_QUEUE, binding.getDestination());
		assertEquals(RabbitDocumentProcessingConfig.PROCESSING_EXCHANGE, binding.getExchange());
		assertEquals(RabbitDocumentProcessingConfig.PROCESSING_ROUTING_KEY, binding.getRoutingKey());
	}

	@Test
	void declaresDurableDeadLetterTopology() {
		var exchange = config.documentProcessingDeadLetterExchange();
		var queue = config.documentProcessingDeadLetterQueue();
		var binding = config.documentProcessingDeadLetterBinding(queue, exchange);

		assertEquals(RabbitDocumentProcessingConfig.DEAD_LETTER_EXCHANGE, exchange.getName());
		assertTrue(exchange.isDurable());
		assertEquals(RabbitDocumentProcessingConfig.DEAD_LETTER_QUEUE, queue.getName());
		assertTrue(queue.isDurable());
		assertEquals(RabbitDocumentProcessingConfig.DEAD_LETTER_QUEUE, binding.getDestination());
		assertEquals(RabbitDocumentProcessingConfig.DEAD_LETTER_EXCHANGE, binding.getExchange());
		assertEquals(RabbitDocumentProcessingConfig.DEAD_LETTER_ROUTING_KEY, binding.getRoutingKey());
	}

	@Test
	void configuresJacksonMessageConversion() {
		assertInstanceOf(JacksonJsonMessageConverter.class, config.rabbitMessageConverter());
	}

}
