package com.example.demo.controllers;

import com.example.demo.dto.MarkdownResponse;
import com.example.demo.dto.ZipProcessingResult;
import com.example.demo.service.FileTypeDetector;
import com.example.demo.service.FinancialDataService;
import com.example.demo.service.MarkdownConverterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

/**
 * REST Controller for financial data file processing and markdown conversion
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/financial-data")
public class FinancialDataController {
    
    private final FinancialDataService financialDataService;
    private final MarkdownConverterService markdownConverterService;
    private final FileTypeDetector fileTypeDetector;
    
    @Autowired
    public FinancialDataController(
            FinancialDataService financialDataService,
            MarkdownConverterService markdownConverterService,
            FileTypeDetector fileTypeDetector) {
        this.financialDataService = financialDataService;
        this.markdownConverterService = markdownConverterService;
        this.fileTypeDetector = fileTypeDetector;
    }
    
    /**
     * Upload and convert financial data file to markdown
     * Supports single files (CSV, Excel, JSON, TXT) and ZIP archives containing multiple files
     * 
     * @param file The financial data file (CSV, Excel, JSON, TXT, or ZIP)
     * @return MarkdownResponse containing the converted markdown
     */
    @PostMapping(value = "/convert", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MarkdownResponse> convertToMarkdown(
            @RequestParam("file") MultipartFile file) {
        
        log.info("Received file upload request: {}", file.getOriginalFilename());
        
        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        
        String filename = file.getOriginalFilename() != null ? 
            file.getOriginalFilename() : "unknown";
        
        try {
            // Check if it's a ZIP file
            FileTypeDetector.FileType fileType = fileTypeDetector.detectFileType(file);
            
            if (fileType == FileTypeDetector.FileType.ZIP) {
                // Process ZIP file
                ZipProcessingResult zipResult = financialDataService.processZipFile(file);
                
                // Convert ZIP to markdown
                String markdown = markdownConverterService.convertZipToMarkdown(zipResult, filename);
                
                // Build response for ZIP
                MarkdownResponse response = MarkdownResponse.builder()
                    .markdown(markdown)
                    .filename(filename)
                    .recordCount(zipResult.getAllRecords().size())
                    .processedAt(LocalDateTime.now())
                    .fileType(file.getContentType())
                    .status("SUCCESS")
                    .isZipArchive(true)
                    .zipFileContents(zipResult.getFileInfos())
                    .totalFilesInZip(zipResult.getTotalFiles())
                    .successfullyProcessedFiles(zipResult.getSuccessfullyProcessedFiles())
                    .build();
                
                log.info("Successfully processed ZIP file: {} with {} files and {} total records", 
                    filename, zipResult.getTotalFiles(), zipResult.getAllRecords().size());
                
                return ResponseEntity.status(HttpStatus.OK).body(response);
            } else {
                // Process single file
                var records = financialDataService.processFile(file);
                
                // Convert to markdown
                String markdown = markdownConverterService.convertToMarkdown(records, filename);
                
                // Build response
                MarkdownResponse response = MarkdownResponse.builder()
                    .markdown(markdown)
                    .filename(filename)
                    .recordCount(records.size())
                    .processedAt(LocalDateTime.now())
                    .fileType(file.getContentType())
                    .status("SUCCESS")
                    .isZipArchive(false)
                    .build();
                
                log.info("Successfully processed file: {} with {} records", filename, records.size());
                
                return ResponseEntity.status(HttpStatus.OK).body(response);
            }
            
        } catch (Exception e) {
            log.error("Error processing file: {}", filename, e);
            throw e;
        }
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Financial Data API is running");
    }
}
