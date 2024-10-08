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
import com.bsha2nk.message.NotificationDTO;
import com.bsha2nk.message.broker.RabbitMQSender;
import com.bsha2nk.repository.ScanRepository;
import com.bsha2nk.util.URLConstants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ScanStatusPoller {

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private RabbitMQSender rabbitMQSender;

	@Autowired
	private ScanRepository scanRepository;

	@Value("${jwt-token}")
	private String jwtToken;

	@Scheduled(fixedRateString = "${status-check-interval}")
	private void pollStatus() {
		List<Scan> scans = scanRepository.findIncompleteScans();
		if (!scans.isEmpty()) {
			log.info("Found incomplete scans, will poll for current status.");
		} else {
			log.info("All scans completed.");
			return;
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
				try {
					scanStartSuccess(scan, statusResponse);
				} catch (JsonProcessingException e) {
					throw new RuntimeException("Error retrieving status for upload id " + scan.getUploadId());
				}
			}
		});
	}

	private void scanStartSuccess(Scan scan, ResponseEntity<String> statusResponse) throws JsonProcessingException {
		JsonNode jsonNode = mapper.readTree(statusResponse.getBody());
		String status = jsonNode.findValue("progress").asText();
		
		if (status.equals("100")) {
			scan.setStatus("complete");
			int numOfVulnerabilities = jsonNode.findValue("vulnerabilitiesFound").asInt();
			if (numOfVulnerabilities > 3) {
				rabbitMQSender.sendMessage(NotificationDTO.builder()
						.event("Scan complete for CI Upload ID " + scan.getUploadId())
						.message(String.format("Found %s vulnerabilities.", numOfVulnerabilities))
						.build());
			}
		} else {
			scan.setStatus("progress");
			rabbitMQSender.sendMessage(NotificationDTO.builder()
					.event("Scan in progress for CI Upload ID " + scan.getUploadId())
					.message("Scan in progress for CI Upload ID " + scan.getUploadId())
					.build());
		}

		scanRepository.save(scan);
	}

}