package com.bsha2nk.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.bsha2nk.entity.Scan;
import com.bsha2nk.repository.ScanRepository;
import com.bsha2nk.util.URLConstants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class ScanStatusPoller {
	
	@Autowired
	private ObjectMapper mapper;
	
	@Autowired
	private ScanRepository scanRepository;
	
	@Value("${jwt-token}")
	private String jwtToken;

	@Scheduled(fixedRate = 30000)
	private void pollStatus() {
		List<Scan> scans = scanRepository.findIncompleteScans();
		if (!scans.isEmpty()) {
			System.out.println("Found incomplete scans, will poll for current status.");
		}
		
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken);
		
		HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
		
		scans.forEach(scan -> {
			String url = UriComponentsBuilder.fromHttpUrl(URLConstants.SCAN_STATUS_URL)
			        .queryParam("ciUploadId", scan.getUploadId())
			        .queryParam("extendedOutput", "false")
			        .toUriString();

			RestTemplate restTemplate = new RestTemplate();
			ResponseEntity<String> statusResponse = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
			
			if (statusResponse.getStatusCode().is2xxSuccessful()) {
				JsonNode jsonNode;
				try {
					jsonNode = mapper.readTree(statusResponse.getBody());
					String status = jsonNode.findValue("progress").asText();
					if (status.equals("100")) {
						scan.setStatus("complete");
						//TODO Setup a message broker to notification service and send a message indicating completed status fo id
						//Notification service can be a separate service
					} else {
						scan.setStatus("progress");
					}
					scanRepository.save(scan);
				} catch (JsonProcessingException e) {
					throw new RuntimeException("Error retrieving status for upload id " + scan.getUploadId());
				}
			}
			
			System.out.println(statusResponse);
		});
	}
	
}