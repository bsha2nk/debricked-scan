package com.bsha2nk.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.bsha2nk.exception.FileUploadException;
import com.bsha2nk.message.broker.RabbitMQSender;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(SpringExtension.class)
class UploadServiceImplTest {

	@Mock
	private ObjectMapper mapper;
	
	@Mock
	private RestTemplate restTemplate;
	
	@Mock
	private RabbitMQSender rabbitMQSender;
	
	@InjectMocks
	private UploadServiceImpl uploadService;
	
	@Test
	void test_upload_response_isNull() throws FileNotFoundException, IOException {
		when(restTemplate.postForEntity(any(), any(), any())).thenReturn(null);
		
		MultipartFile[] files = new MultipartFile[1];
		File file = new File(UploadServiceImplTest.class.getClassLoader().getResource("test_file.txt").getFile());
		files[0] = new MockMultipartFile("fileName", "fileName", null, new FileInputStream(file));
		
		assertThrows(FileUploadException.class, () -> uploadService.upload(files, "jwt", "repositoryName", "commitName"));
	}
	
//	@Test
	void test_upload_responseCode_not_success() throws FileNotFoundException, IOException {
		ResponseEntity responseEntity = new ResponseEntity<>(HttpStatusCode.valueOf(300));
		when(restTemplate.postForEntity(any(), any(), any())).thenReturn(responseEntity);
		
		MultipartFile[] files = new MultipartFile[1];
		File file = new File(UploadServiceImplTest.class.getClassLoader().getResource("test_file.txt").getFile());
		files[0] = new MockMultipartFile("fileName", "fileName", null, new FileInputStream(file));
		
		assertThrows(FileUploadException.class, () -> uploadService.upload(files, "jwt", "repositoryName", "commitName"));
	}
	
}