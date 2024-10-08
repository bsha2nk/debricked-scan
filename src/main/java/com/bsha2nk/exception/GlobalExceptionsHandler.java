package com.bsha2nk.exception;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
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

	@ExceptionHandler(HandlerMethodValidationException.class)
	public ResponseEntity<ErrorMessage> handleValidationExceptions(HandlerMethodValidationException e) {
		StringBuilder sb = new StringBuilder();
		
		e.getAllValidationResults().forEach(res -> {
			res.getResolvableErrors().forEach(err -> {
				var paramName = ((DefaultMessageSourceResolvable)(err.getArguments()[0]));
				sb.append(paramName.getDefaultMessage() + " " + err.getDefaultMessage() + ", ");
			});
		});
		
		ErrorMessage message = new ErrorMessage();
		message.setMessage(sb.toString());
		
		return ResponseEntity.badRequest().body(message);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorMessage> allExceptions(Exception e) {
		ErrorMessage message = new ErrorMessage();
		message.setMessage(e.getMessage());

		return ResponseEntity.internalServerError().body(message);
	}

}