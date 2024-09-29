package com.bsha2nk.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.bsha2nk.service.UploadService;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

@RestController
@RequestMapping(value = "/api/v1")
public class ScanFilesController {
	
	private UploadService uploadToDebricked;
	
	public ScanFilesController(UploadService uploadToDebricked) {
		this.uploadToDebricked = uploadToDebricked; 
	}
	
	@PostMapping(value = "/files/upload", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
	public ResponseEntity<String> uploadFiles(@RequestParam @NotEmpty MultipartFile[] files, @RequestParam @NotBlank String jwtToken,
			@RequestParam @NotBlank String repositoryName, @RequestParam @NotBlank String commitName) throws Exception {
		try {
			String response = uploadToDebricked.upload(files, jwtToken, repositoryName, commitName);			
			return ResponseEntity.ok(response);
		} catch(Exception e) {
			throw new Exception(e.getMessage());
		}
		
	}

}