package com.bsha2nk.message;

import java.io.Serializable;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationDTO implements Serializable {
	
	private static final long serialVersionUID = 1L;

	private String event;
	
	private String message;
	
}