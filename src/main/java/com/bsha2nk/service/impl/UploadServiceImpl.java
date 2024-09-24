package com.bsha2nk.service.impl;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.bsha2nk.exception.FileUploadException;
import com.bsha2nk.service.UploadService;
import com.bsha2nk.util.URLConstants;
import com.bsha2nk.util.Utilities;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class UploadServiceImpl implements UploadService {

	@Autowired
	private ObjectMapper mapper;
	
	@Autowired
	private RestTemplate restTemplate;
	
	@Autowired
	private ScanServiceImpl scanService;
	
	@Override
	public String upload(MultipartFile[] files, String jwtToken, String repositoryName, String commitName) throws Exception {
		String ciUploadId = "";

		HttpHeaders headers = Utilities.getMultipartHeaders(jwtToken);

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
			
			ResponseEntity<String> response;
			try {
				response = restTemplate.postForEntity(URLConstants.UPLOAD_URL, requestEntity, String.class);	
			} catch (Exception e) {
				System.out.println("File with name " + file.getOriginalFilename() + " could not be uploaded. Scan will not be started.");
				throw new FileUploadException("File with name " + file.getOriginalFilename() + " could not be uploaded. Scan will not be started.");
			}

			if (Objects.nonNull(response) && response.getStatusCode().is2xxSuccessful()) {
				System.out.println("File upload status for file " + file.getOriginalFilename() + ": " + response.getStatusCode());
				if (ciUploadId.isBlank()) {
					JsonNode jsonNode = mapper.readTree(response.getBody());
					ciUploadId = jsonNode.findValue("ciUploadId").asText();
				}
			} else {
				System.out.println("File with name " + file.getOriginalFilename() + " could not be uploaded. Scan will not be started.");
				throw new FileUploadException("File with name " + file.getOriginalFilename() + " could not be uploaded. Scan will not be started.");
			}

		}

		return scanService.startScan(ciUploadId, jwtToken);
	}

	private String createTempFile(MultipartFile file) throws IOException {
		String tempFileName = "/tmp/" + file.getOriginalFilename();

		try (FileOutputStream fo = new FileOutputStream(tempFileName)) {
			fo.write(file.getBytes());
		}

		return tempFileName;
	}

}