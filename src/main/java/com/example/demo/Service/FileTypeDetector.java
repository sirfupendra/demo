package com.example.demo.service;

import com.example.demo.exception.UnsupportedFileFormatException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Utility class to detect file types
 */
@Slf4j
@Component
public class FileTypeDetector {
    
    public enum FileType {
        CSV("text/csv", "csv"),
        EXCEL_XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),
        EXCEL_XLS("application/vnd.ms-excel", "xls"),
        JSON("application/json", "json"),
        TEXT("text/plain", "txt"),
        ZIP("application/zip", "zip");
        
        private final String mimeType;
        private final String extension;
        
        FileType(String mimeType, String extension) {
            this.mimeType = mimeType;
            this.extension = extension;
        }
        
        public String getMimeType() {
            return mimeType;
        }
        
        public String getExtension() {
            return extension;
        }
    }
    
    public FileType detectFileType(MultipartFile file) {
        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();
        
        if (filename == null || filename.isEmpty()) {
            throw new UnsupportedFileFormatException("Filename is empty or null");
        }
        
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        
        // Check by MIME type first
        if (contentType != null) {
            for (FileType type : FileType.values()) {
                if (contentType.equalsIgnoreCase(type.mimeType)) {
                    log.debug("Detected file type {} by MIME type: {}", type, contentType);
                    return type;
                }
            }
        }
        
        // Fallback to extension
        for (FileType type : FileType.values()) {
            if (type.extension.equalsIgnoreCase(extension)) {
                log.debug("Detected file type {} by extension: {}", type, extension);
                return type;
            }
        }
        
        // Try to detect JSON by content
        if (extension.equals("json") || contentType != null && contentType.contains("json")) {
            return FileType.JSON;
        }
        
        // Check for ZIP file
        if (extension.equals("zip") || 
            (contentType != null && (contentType.equals("application/zip") || contentType.equals("application/x-zip-compressed")))) {
            return FileType.ZIP;
        }
        
        throw new UnsupportedFileFormatException(
            String.format("Unsupported file format. Extension: %s, ContentType: %s", extension, contentType));
    }
    
    public boolean isSupported(MultipartFile file) {
        try {
            detectFileType(file);
            return true;
        } catch (UnsupportedFileFormatException e) {
            return false;
        }
    }
}
