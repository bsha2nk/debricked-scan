package com.bsha2nk.exception;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@ControllerAdvice
public class GlobalExceptionsHandler {
	
	@ExceptionHandler(MissingServletRequestPartException.class)
	public ResponseEntity<ErrorMessage> missingParams(MissingServletRequestPartException e) {
		ErrorMessage message = new ErrorMessage();
		message.setMessage(e.getMessage());
		
		return ResponseEntity.badRequest().body(message);
	}
	
	@ExceptionHandler(ScanStartException.class)
	public ResponseEntity<ErrorMessage> missingParams(ScanStartException e) {
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