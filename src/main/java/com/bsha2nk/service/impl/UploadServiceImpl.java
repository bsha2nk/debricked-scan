package com.bsha2nk.service.impl;

import java.io.FileOutputStream;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.bsha2nk.entity.Scan;
import com.bsha2nk.repository.ScanRepository;
import com.bsha2nk.service.UploadService;
import com.bsha2nk.util.URLConstants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class UploadServiceImpl implements UploadService {

	@Autowired
	private ObjectMapper mapper;
	
	@Autowired
	private ScanRepository scanRepository;

	@Override
	public String upload(MultipartFile[] files, String jwtToken, String repositoryName, String commitName) throws IOException {
		String ciUploadId = "";

		HttpHeaders headers = getUploadHeaders(jwtToken);

		for (MultipartFile file : files) {
			String tempFileName = createTempFile(file);

			MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
			body.add("fileData", new FileSystemResource(tempFileName));
			body.add("repositoryName", repositoryName);
			body.add("commitName", commitName);

			if (!ciUploadId.isBlank()) {
				body.add("ciUploadId", ciUploadId);
			}

			HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

			RestTemplate restTemplate = new RestTemplate();
			ResponseEntity<String> response = restTemplate.postForEntity(URLConstants.UPLOAD_URL, requestEntity, String.class);

			if (response.getStatusCode().is2xxSuccessful() && ciUploadId.isBlank()) {
				JsonNode jsonNode = mapper.readTree(response.getBody());
				ciUploadId = jsonNode.findValue("ciUploadId").asText();
			}

			System.out.println("Files upload status: " + response.getStatusCode());
		}

		return startScan(ciUploadId, jwtToken);
	}

	private String startScan(String ciUploadId, String jwtToken) throws JsonProcessingException {
		HttpHeaders headers = getUploadHeaders(jwtToken);

		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add("ciUploadId", ciUploadId);
		body.add("returnCommitData", true);

		HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<String> response = restTemplate.postForEntity(URLConstants.START_SCAN_URL, requestEntity, String.class);

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
		}

		return response.toString();
	}
	
	private HttpHeaders getUploadHeaders(String jwtToken) {
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken);
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);

		return headers;
	}

	private String createTempFile(MultipartFile file) throws IOException {
		String tempFileName = "/tmp/" + file.getOriginalFilename();

		try (FileOutputStream fo = new FileOutputStream(tempFileName)) {
			fo.write(file.getBytes());
		}

		return tempFileName;
	}

}