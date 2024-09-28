package com.bsha2nk.service.impl;

import java.io.IOException;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.bsha2nk.exception.FileUploadException;
import com.bsha2nk.message.NotificationDTO;
import com.bsha2nk.message.broker.RabbitMQSender;
import com.bsha2nk.service.UploadService;
import com.bsha2nk.util.URLConstants;
import com.bsha2nk.util.Utilities;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UploadServiceImpl implements UploadService {

	@Autowired
	private ObjectMapper mapper;
	
	@Autowired
	private RestTemplate restTemplate;
	
	@Autowired
	private RabbitMQSender rabbitMQSender;
	
	@Autowired
	private ScanServiceImpl scanService;
	
	@Override
	public String upload(MultipartFile[] files, String jwtToken, String repositoryName, String commitName) throws IOException {
		String ciUploadId = "";

		HttpHeaders headers = Utilities.getMultipartHeaders(jwtToken);

		for (MultipartFile file : files) {
			MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
			body.add("fileData", file.getResource());
			body.add("repositoryName", repositoryName);
			body.add("commitName", commitName);

			if (!ciUploadId.isBlank()) {
				body.add("ciUploadId", ciUploadId);
			}

			HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
			
			ResponseEntity<String> response;
			try {
				response = restTemplate.postForEntity(URLConstants.UPLOAD_URL, requestEntity, String.class);	
			} catch (HttpClientErrorException e) {
				String error = "File(s) could not be uploaded. The presented JWT may not be valid. Scan will not be started.";
				sendErrorNotification(ciUploadId, file.getOriginalFilename(), error);
				throw new FileUploadException(error);
			} catch (Exception e) {
				String error = "File with name " + file.getOriginalFilename() + " could not be uploaded. Scan will not be started.";
				sendErrorNotification(ciUploadId, file.getOriginalFilename(), error);
				throw new FileUploadException(error);
			}

			if (Objects.nonNull(response) && response.getStatusCode().is2xxSuccessful()) {
				log.info("File upload status for file " + file.getOriginalFilename() + ": " + response.getStatusCode());
				if (ciUploadId.isBlank()) {
					JsonNode jsonNode = mapper.readTree(response.getBody());
					ciUploadId = jsonNode.findValue("ciUploadId").asText();
				}
			} else {
				String error = "File with name " + file.getOriginalFilename() + " could not be uploaded. Scan will not be started.";
				sendErrorNotification(ciUploadId, file.getOriginalFilename(), error);
				throw new FileUploadException(error);
			}
			
		}
		
		rabbitMQSender.sendMessage(NotificationDTO.builder()
				.event("Upload Successful for CI Upload ID " + ciUploadId)
				.message(String.format("All files were uploaded successfully with ci-uploadId %s. Scan will be triggered.", ciUploadId))
				.build());

		return scanService.startScan(ciUploadId, jwtToken);
	}

	private void sendErrorNotification(String ciUploadId, String fileName, String error) {
		log.warn(error);
		
		rabbitMQSender.sendMessage(NotificationDTO.builder()
				.event("Upload Unsuccessful" + (ciUploadId.isBlank() ? " for file " + fileName : " for CI Upload ID " + ciUploadId))
				.message(error)
				.build());
	}

}