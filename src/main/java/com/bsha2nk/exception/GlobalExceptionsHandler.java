package com.bsha2nk.exception;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import com.bsha2nk.message.NotificationDTO;
import com.bsha2nk.message.broker.RabbitMQSender;

@ControllerAdvice
public class GlobalExceptionsHandler {
	
	@Autowired
	private RabbitMQSender rabbitMQSender;
	
	@ExceptionHandler(MissingServletRequestPartException.class)
	public ResponseEntity<ErrorMessage> missingParams(MissingServletRequestPartException e) {
		ErrorMessage message = new ErrorMessage();
		message.setMessage(e.getMessage());
		
		return ResponseEntity.badRequest().body(message);
	}
	
	@ExceptionHandler(FileUploadException.class)
	public ResponseEntity<ErrorMessage> fileUploadError(FileUploadException e) {
		ErrorMessage message = new ErrorMessage();
		message.setMessage(e.getMessage());
		
		return new ResponseEntity<>(message, HttpStatusCode.valueOf(503));
	}
	
	@ExceptionHandler(ScanStartException.class)
	public ResponseEntity<ErrorMessage> startScanError(ScanStartException e) {
		rabbitMQSender.sendMessage(NotificationDTO.builder()
				.event("Scan could not be started")
				.message(e.getMessage())
				.build());
		
		ErrorMessage message = new ErrorMessage();
		message.setMessage(e.getMessage());
		
		return new ResponseEntity<>(message, HttpStatusCode.valueOf(503));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorMessage> allExceptions(Exception e) {
		ErrorMessage message = new ErrorMessage();
		message.setMessage(e.getMessage());

		return ResponseEntity.internalServerError().body(message);
	}
	
}