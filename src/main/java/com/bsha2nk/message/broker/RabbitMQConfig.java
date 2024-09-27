package com.bsha2nk.message.broker;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;

@Configuration
@Getter
public class RabbitMQConfig {

	@Value("${notification.queue}")
	private String notificationExchange;

	@Value("${notification.exchange}")
	private String notificationQueue;

	@Value("${notification.routing-key}")
	private String notificationRoutingKey;

	@Bean
	TopicExchange notificationExchange() {
		return new TopicExchange(notificationExchange);
	}

	@Bean
	Queue notificationQueue() {
		return new Queue(notificationQueue, true);
	}

	@Bean
	Binding binding(Queue queue, TopicExchange exchange) {
		return BindingBuilder.bind(queue).to(exchange).with(notificationRoutingKey);
	}

}
