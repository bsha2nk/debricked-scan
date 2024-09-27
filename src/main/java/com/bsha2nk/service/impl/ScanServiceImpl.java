package com.bsha2nk.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.bsha2nk.entity.Scan;
import com.bsha2nk.exception.ScanStartException;
import com.bsha2nk.message.NotificationDTO;
import com.bsha2nk.message.broker.RabbitMQSender;
import com.bsha2nk.repository.ScanRepository;
import com.bsha2nk.util.URLConstants;
import com.bsha2nk.util.Utilities;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ScanServiceImpl {
	
	@Autowired
	private ObjectMapper mapper;
	
	@Autowired
	private RestTemplate restTemplate;
	
	@Autowired
	private RabbitMQSender rabbitMQSender;
	
	@Autowired
	private ScanRepository scanRepository;

	public String startScan(String ciUploadId, String jwtToken) throws JsonProcessingException {
		HttpHeaders headers = Utilities.getMultipartHeaders(jwtToken);

		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add("ciUploadId", ciUploadId);
		body.add("returnCommitData", true);

		HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

		ResponseEntity<String> response;
		try {
			response = restTemplate.postForEntity(URLConstants.START_SCAN_URL, requestEntity, String.class);
		} catch(Exception e) {
			String error = "Something went wrong while trying to start your scan for id " + ciUploadId;
			sendErrorNotification(ciUploadId, error);
			throw new ScanStartException(error);
		}

		if (response.getStatusCode().is2xxSuccessful()) {
			JsonNode jsonNode = mapper.readTree(response.getBody());
			String repositoryId = jsonNode.findValue("repositoryId").asText();
			String commitId = jsonNode.findValue("commitId").asText();
			
			Scan scan = new Scan();
			scan.setStatus("start");
			scan.setUploadId(ciUploadId);
			scan.setRepositoryId(repositoryId);
			scan.setCommitId(commitId);
			
			scanRepository.save(scan);
			
			rabbitMQSender.sendMessage(NotificationDTO.builder()
					.event("Scan started for CI Upload ID " + ciUploadId)
					.message(String.format("Scan started for files with repositoryId %s and commitId %s and ci-uploadId %s.", repositoryId, commitId, ciUploadId))
					.build());
			
			String successResponse = String.format("Files were uploaded successfully and scan started with repositoryId %s and commitId %s and ci-uploadId %s.",
					repositoryId, commitId, ciUploadId); 
			log.info(successResponse);
			return successResponse; 
		} else {
			String error = "Scan for id " + ciUploadId + " could not be started. Response code " + response.getStatusCode();
			sendErrorNotification(ciUploadId, error);
			throw new ScanStartException(error);
		}

	}
	
	private void sendErrorNotification(String ciUploadId, String error) {
		log.warn(error);
		
		rabbitMQSender.sendMessage(NotificationDTO.builder()
				.event("Upload Unsuccessful" + (ciUploadId.isBlank() ? "" : " for CI Upload ID " + ciUploadId))
				.message(error)
				.build());
	}
	
}