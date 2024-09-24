package com.bsha2nk.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.bsha2nk.entity.Scan;

public interface ScanRepository extends JpaRepository<Scan, Long> {

	@Query(value = "SELECT * FROM scans where status <> 'complete'", nativeQuery = true)
	List<Scan> findIncompleteScans();
	
}