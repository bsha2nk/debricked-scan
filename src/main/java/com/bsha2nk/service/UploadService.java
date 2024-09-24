package com.bsha2nk.service;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

public interface UploadService {

	String upload(MultipartFile[] files, String jwtToken, String repositoryName, String commitName) throws Exception;

}