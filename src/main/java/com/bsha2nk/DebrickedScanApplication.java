package com.bsha2nk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootApplication
@EnableScheduling
public class DebrickedScanApplication {

	public static void main(String[] args) {
		SpringApplication.run(DebrickedScanApplication.class, args);
	}

    @Bean
    ObjectMapper getObjetcMapper() {
		return new ObjectMapper();
	}
    
    @Bean
    RestTemplate getRestTemplate() {
    	return new RestTemplate();
    }
    
}