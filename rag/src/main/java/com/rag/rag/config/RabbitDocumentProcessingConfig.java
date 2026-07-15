package com.rag.rag.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class RabbitDocumentProcessingConfig {

	public static final String PROCESSING_EXCHANGE = "rag.document-processing";
	public static final String PROCESSING_QUEUE = "rag.document-processing";
	public static final String PROCESSING_ROUTING_KEY = "document.process";
	public static final String DEAD_LETTER_EXCHANGE = "rag.document-processing.dlx";
	public static final String DEAD_LETTER_QUEUE = "rag.document-processing.dlq";
	public static final String DEAD_LETTER_ROUTING_KEY = "document.process.failed";

	@Bean
	DirectExchange documentProcessingExchange() {
		return new DirectExchange(PROCESSING_EXCHANGE, true, false);
	}

	@Bean
	Queue documentProcessingQueue() {
		return QueueBuilder.durable(PROCESSING_QUEUE)
			.deadLetterExchange(DEAD_LETTER_EXCHANGE)
			.deadLetterRoutingKey(DEAD_LETTER_ROUTING_KEY)
			.build();
	}

	@Bean
	Binding documentProcessingBinding(
		@Qualifier("documentProcessingQueue") Queue queue,
		@Qualifier("documentProcessingExchange") DirectExchange exchange) {
		return BindingBuilder.bind(queue).to(exchange).with(PROCESSING_ROUTING_KEY);
	}

	@Bean
	DirectExchange documentProcessingDeadLetterExchange() {
		return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
	}

	@Bean
	Queue documentProcessingDeadLetterQueue() {
		return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
	}

	@Bean
	Binding documentProcessingDeadLetterBinding(
		@Qualifier("documentProcessingDeadLetterQueue") Queue queue,
		@Qualifier("documentProcessingDeadLetterExchange") DirectExchange exchange) {
		return BindingBuilder.bind(queue).to(exchange).with(DEAD_LETTER_ROUTING_KEY);
	}

	@Bean
	MessageConverter rabbitMessageConverter() {
		return new JacksonJsonMessageConverter("com.rag.rag.adapter.messaging");
	}

}
