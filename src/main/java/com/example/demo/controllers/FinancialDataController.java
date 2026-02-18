package com.example.demo.controllers;

import com.example.demo.dto.ZipProcessingResult;
import com.example.demo.service.FileTypeDetector;
import com.example.demo.service.FinancialDataService;
import com.example.demo.service.MarkdownConverterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
     * @return Downloadable markdown file (.md)
     */
    @PostMapping(value = "/convert", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, 
                 produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> convertToMarkdown(
            @RequestParam("file") MultipartFile file) {
        
        log.info("Received file upload request: {}", file.getOriginalFilename());
        
        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        
        String filename = file.getOriginalFilename() != null ? 
            file.getOriginalFilename() : "unknown";
        
        try {
            String markdown;
            String outputFilename;
            
            // Check if it's a ZIP file
            FileTypeDetector.FileType fileType = fileTypeDetector.detectFileType(file);
            
            if (fileType == FileTypeDetector.FileType.ZIP) {
                // Process ZIP file
                ZipProcessingResult zipResult = financialDataService.processZipFile(file);
                
                // Convert ZIP to markdown
                markdown = markdownConverterService.convertZipToMarkdown(zipResult, filename);
                
                // Generate output filename
                String baseName = filename.replaceAll("\\.[^.]*$", ""); // Remove extension
                outputFilename = String.format("%s_report_%s.md", 
                    baseName, 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
                
                log.info("Successfully processed ZIP file: {} with {} files and {} total records", 
                    filename, zipResult.getTotalFiles(), zipResult.getAllRecords().size());
            } else {
                // Process single file
                var records = financialDataService.processFile(file);
                
                // Convert to markdown
                markdown = markdownConverterService.convertToMarkdown(records, filename);
                
                // Generate output filename
                String baseName = filename.replaceAll("\\.[^.]*$", ""); // Remove extension
                outputFilename = String.format("%s_report_%s.md", 
                    baseName, 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
                
                log.info("Successfully processed file: {} with {} records", filename, records.size());
            }
            
            // Convert markdown string to bytes
            byte[] markdownBytes = markdown.getBytes(StandardCharsets.UTF_8);
            ByteArrayResource resource = new ByteArrayResource(markdownBytes);
            
            // Set headers for file download
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, 
                String.format("attachment; filename=\"%s\"", outputFilename));
            headers.add(HttpHeaders.CONTENT_TYPE, "text/markdown; charset=utf-8");
            headers.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(markdownBytes.length));
            
            return ResponseEntity.ok()
                .headers(headers)
                .contentLength(markdownBytes.length)
                .contentType(MediaType.parseMediaType("text/markdown; charset=utf-8"))
                .body(resource);
            
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
