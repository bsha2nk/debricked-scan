package com.bsha2nk.message.broker;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bsha2nk.message.NotificationDTO;

@Service
public class RabbitMQSender {

	@Autowired
    private RabbitTemplate rabbitTemplate;

	@Autowired
    private RabbitMQConfig rabbitMQConfig;
	
	public void sendMessage(NotificationDTO dto) {
		rabbitTemplate.convertAndSend(rabbitMQConfig.getNotificationExchange(), rabbitMQConfig.getNotificationRoutingKey(), dto);
	}
	
}