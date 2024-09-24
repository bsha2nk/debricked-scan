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
import com.bsha2nk.repository.ScanRepository;
import com.bsha2nk.util.URLConstants;
import com.bsha2nk.util.Utilities;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ScanServiceImpl {
	
	@Autowired
	private ObjectMapper mapper;
	
	@Autowired
	private RestTemplate restTemplate;
	
	@Autowired
	private ScanRepository scanRepository;

	public String startScan(String ciUploadId, String jwtToken) throws Exception {
		HttpHeaders headers = Utilities.getMultipartHeaders(jwtToken);

		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add("ciUploadId", ciUploadId);
		body.add("returnCommitData", true);

		HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

		ResponseEntity<String> response;
		try {
			response = restTemplate.postForEntity(URLConstants.START_SCAN_URL, requestEntity, String.class);
		} catch(Exception e) {
			throw new ScanStartException("Something went wrong while trying to start your scan for id " + ciUploadId);
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
			
			return String.format("Files were uploaded successfully and scan started with repositoryId %s and commitId %s and ci-uploadId %s.", repositoryId, commitId, ciUploadId); 
		} else {
			throw new ScanStartException("Scan for id " + ciUploadId + " could not be started. Response code " + response.getStatusCode());
		}

	}
	
}