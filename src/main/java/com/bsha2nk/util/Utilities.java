package com.bsha2nk.util;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

public class Utilities {
	
	private Utilities() {}

	public static HttpHeaders getMultipartHeaders(String jwtToken) {
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken);
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);

		return headers;
	}
	
}