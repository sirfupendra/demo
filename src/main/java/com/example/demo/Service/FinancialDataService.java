package com.example.demo.service;

import com.example.demo.dto.FinancialDataRecord;
import com.example.demo.dto.ZipFileInfo;
import com.example.demo.dto.ZipProcessingResult;
import com.example.demo.exception.FileProcessingException;
import com.example.demo.exception.UnsupportedFileFormatException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Service for processing financial data from various file formats
 */
@Slf4j
@Service
public class FinancialDataService {
    
    private final FileTypeDetector fileTypeDetector;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public FinancialDataService(FileTypeDetector fileTypeDetector, ObjectMapper objectMapper) {
        this.fileTypeDetector = fileTypeDetector;
        this.objectMapper = objectMapper;
    }
    
    public List<FinancialDataRecord> processFile(MultipartFile file) {
        FileTypeDetector.FileType fileType = fileTypeDetector.detectFileType(file);
        
        log.info("Processing file: {} with type: {}", file.getOriginalFilename(), fileType);
        
        return switch (fileType) {
            case CSV -> processCsvFile(file);
            case EXCEL_XLSX, EXCEL_XLS -> processExcelFile(file);
            case JSON -> processJsonFile(file);
            case TEXT -> processTextFile(file);
            case ZIP -> {
                ZipProcessingResult result = processZipFile(file);
                yield result.getAllRecords();
            }
        };
    }
    
    public ZipProcessingResult processZipFile(MultipartFile zipFile) {
        ZipProcessingResult result = new ZipProcessingResult();
        
        try (ZipInputStream zipInputStream = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry entry;
            int fileCount = 0;
            
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                
                String entryName = entry.getName();
                fileCount++;
                result.setTotalFiles(fileCount);
                
                log.info("Processing file from ZIP: {}", entryName);
                
                // Read the entry content
                byte[] buffer = new byte[1024];
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                int len;
                while ((len = zipInputStream.read(buffer)) > 0) {
                    baos.write(buffer, 0, len);
                }
                byte[] fileContent = baos.toByteArray();
                
                // Create a MultipartFile-like wrapper for the extracted file
                ExtractedFile extractedFile = new ExtractedFile(entryName, fileContent);
                
                // Try to process the file
                ZipFileInfo fileInfo = ZipFileInfo.builder()
                    .filename(entryName)
                    .processed(false)
                    .build();
                
                try {
                    // Detect file type and process
                    FileTypeDetector.FileType entryFileType = fileTypeDetector.detectFileType(extractedFile);
                    fileInfo.setFileType(entryFileType.name());
                    
                    // Check if file type is supported (exclude ZIP files - nested ZIPs are not supported)
                    if (entryFileType == FileTypeDetector.FileType.ZIP) {
                        log.warn("Nested ZIP files are not supported. Skipping file: {}", entryName);
                        fileInfo.setErrorMessage("Nested ZIP files are not supported");
                        fileInfo.setProcessed(false);
                    } else {
                        // Process supported file types
                        List<FinancialDataRecord> records = switch (entryFileType) {
                            case CSV -> processCsvFile(extractedFile);
                            case EXCEL_XLSX, EXCEL_XLS -> processExcelFile(extractedFile);
                            case JSON -> processJsonFile(extractedFile);
                            case TEXT -> processTextFile(extractedFile);
                            default -> {
                                log.warn("Unsupported file type in ZIP: {} for file: {}", entryFileType, entryName);
                                yield null; // Use null to indicate unsupported type
                            }
                        };
                        
                        // Only mark as processed if records were successfully extracted
                        if (records != null) {
                            fileInfo.setRecordCount(records.size());
                            fileInfo.setProcessed(true);
                            result.getAllRecords().addAll(records);
                            result.setSuccessfullyProcessedFiles(result.getSuccessfullyProcessedFiles() + 1);
                            
                            log.info("Successfully processed {} records from ZIP file: {}", records.size(), entryName);
                        } else {
                            // Unsupported file type
                            log.warn("File type {} is not supported for processing. File: {}", entryFileType, entryName);
                            fileInfo.setErrorMessage("Unsupported file type: " + entryFileType);
                            fileInfo.setProcessed(false);
                        }
                    }
                    
                } catch (UnsupportedFileFormatException e) {
                    // File type detection failed - unsupported format
                    log.warn("Unsupported file format in ZIP: {} - {}", entryName, e.getMessage());
                    fileInfo.setErrorMessage("Unsupported file format: " + e.getMessage());
                    fileInfo.setProcessed(false);
                } catch (Exception e) {
                    // Other processing errors
                    log.error("Error processing file {} from ZIP: {}", entryName, e.getMessage(), e);
                    fileInfo.setErrorMessage(e.getMessage());
                    fileInfo.setProcessed(false);
                }
                
                result.getFileInfos().add(fileInfo);
                zipInputStream.closeEntry();
            }
            
            log.info("ZIP processing complete. Total files: {}, Successfully processed: {}, Total records: {}", 
                result.getTotalFiles(), result.getSuccessfullyProcessedFiles(), result.getAllRecords().size());
            
        } catch (Exception e) {
            throw new FileProcessingException("Error processing ZIP file: " + e.getMessage(), e);
        }
        
        return result;
    }
    
    /**
     * Helper class to wrap extracted ZIP file entries as MultipartFile-like objects
     */
    private static class ExtractedFile implements MultipartFile {
        private final String filename;
        private final byte[] content;
        
        public ExtractedFile(String filename, byte[] content) {
            this.filename = filename;
            this.content = content;
        }
        
        @Override
        public String getName() {
            return "file";
        }
        
        @Override
        public String getOriginalFilename() {
            return filename;
        }
        
        @Override
        public String getContentType() {
            // Try to determine content type from extension
            if (filename.toLowerCase().endsWith(".csv")) {
                return "text/csv";
            } else if (filename.toLowerCase().endsWith(".xlsx")) {
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            } else if (filename.toLowerCase().endsWith(".xls")) {
                return "application/vnd.ms-excel";
            } else if (filename.toLowerCase().endsWith(".json")) {
                return "application/json";
            } else if (filename.toLowerCase().endsWith(".txt")) {
                return "text/plain";
            }
            return "application/octet-stream";
        }
        
        @Override
        public boolean isEmpty() {
            return content == null || content.length == 0;
        }
        
        @Override
        public long getSize() {
            return content != null ? content.length : 0;
        }
        
        @Override
        public byte[] getBytes() {
            return content != null ? content : new byte[0];
        }
        
        @Override
        public java.io.InputStream getInputStream() {
            return new ByteArrayInputStream(content != null ? content : new byte[0]);
        }
        
        @Override
        public void transferTo(java.io.File dest) throws IllegalStateException {
            throw new UnsupportedOperationException("transferTo not supported for extracted files");
        }
    }
    
    private List<FinancialDataRecord> processCsvFile(MultipartFile file) {
        List<FinancialDataRecord> records = new ArrayList<>();
        
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            String[] headers = reader.readNext();
            if (headers == null) {
                throw new FileProcessingException("CSV file is empty");
            }
            
            String[] line;
            while ((line = reader.readNext()) != null) {
                Map<String, Object> fields = new LinkedHashMap<>();
                for (int i = 0; i < headers.length && i < line.length; i++) {
                    fields.put(headers[i].trim(), line[i].trim());
                }
                
                FinancialDataRecord record = parseFinancialRecord(fields);
                records.add(record);
            }
            
            log.info("Processed {} records from CSV file", records.size());
        } catch (Exception e) {
            throw new FileProcessingException("Error processing CSV file: " + e.getMessage(), e);
        }
        
        return records;
    }
    
    private List<FinancialDataRecord> processExcelFile(MultipartFile file) {
        List<FinancialDataRecord> records = new ArrayList<>();
        
        try (Workbook workbook = createWorkbook(file)) {
            Sheet sheet = workbook.getSheetAt(0);
            
            if (sheet.getPhysicalNumberOfRows() == 0) {
                throw new FileProcessingException("Excel file is empty");
            }
            
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new FileProcessingException("Excel file has no header row");
            }
            
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(getCellValueAsString(cell));
            }
            
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                Map<String, Object> fields = new LinkedHashMap<>();
                for (int j = 0; j < headers.size(); j++) {
                    Cell cell = row.getCell(j);
                    fields.put(headers.get(j), getCellValueAsString(cell));
                }
                
                FinancialDataRecord record = parseFinancialRecord(fields);
                records.add(record);
            }
            
            log.info("Processed {} records from Excel file", records.size());
        } catch (Exception e) {
            throw new FileProcessingException("Error processing Excel file: " + e.getMessage(), e);
        }
        
        return records;
    }
    
    private Workbook createWorkbook(MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename();
        if (filename != null && filename.endsWith(".xlsx")) {
            return new XSSFWorkbook(file.getInputStream());
        } else {
            return new HSSFWorkbook(file.getInputStream());
        }
    }
    
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue().toString();
                } else {
                    yield String.valueOf(cell.getNumericCellValue());
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }
    
    private List<FinancialDataRecord> processJsonFile(MultipartFile file) {
        List<FinancialDataRecord> records = new ArrayList<>();
        
        try {
            String content = new String(file.getBytes());
            
            // Try to parse as array first
            List<Map<String, Object>> dataList = objectMapper.readValue(
                content, new TypeReference<List<Map<String, Object>>>() {});
            
            for (Map<String, Object> data : dataList) {
                FinancialDataRecord record = parseFinancialRecord(data);
                records.add(record);
            }
            
            log.info("Processed {} records from JSON file", records.size());
        } catch (Exception e) {
            throw new FileProcessingException("Error processing JSON file: " + e.getMessage(), e);
        }
        
        return records;
    }
    
    private List<FinancialDataRecord> processTextFile(MultipartFile file) {
        List<FinancialDataRecord> records = new ArrayList<>();
        
        try {
            String content = new String(file.getBytes());
            String[] lines = content.split("\n");
            
            if (lines.length == 0) {
                throw new FileProcessingException("Text file is empty");
            }
            
            // Assume first line is header
            String[] headers = lines[0].split("\t|,|\\|");
            
            for (int i = 1; i < lines.length; i++) {
                String[] values = lines[i].split("\t|,|\\|");
                Map<String, Object> fields = new LinkedHashMap<>();
                
                for (int j = 0; j < headers.length && j < values.length; j++) {
                    fields.put(headers[j].trim(), values[j].trim());
                }
                
                FinancialDataRecord record = parseFinancialRecord(fields);
                records.add(record);
            }
            
            log.info("Processed {} records from text file", records.size());
        } catch (Exception e) {
            throw new FileProcessingException("Error processing text file: " + e.getMessage(), e);
        }
        
        return records;
    }
    
    private FinancialDataRecord parseFinancialRecord(Map<String, Object> fields) {
        FinancialDataRecord record = new FinancialDataRecord(fields);
        
        // Try to extract common financial fields
        fields.forEach((key, value) -> {
            String lowerKey = key.toLowerCase();
            String strValue = value != null ? value.toString() : "";
            
            if (lowerKey.contains("date")) {
                try {
                    record.setDate(parseDate(strValue));
                } catch (Exception e) {
                    log.debug("Could not parse date from field {}: {}", key, strValue);
                }
            }
            
            if (lowerKey.contains("amount") || lowerKey.contains("value") || 
                lowerKey.contains("price") || lowerKey.contains("balance")) {
                try {
                    record.setAmount(parseAmount(strValue));
                } catch (Exception e) {
                    log.debug("Could not parse amount from field {}: {}", key, strValue);
                }
            }
            
            if (lowerKey.contains("description") || lowerKey.contains("note") || 
                lowerKey.contains("memo")) {
                record.setDescription(strValue);
            }
            
            if (lowerKey.contains("category") || lowerKey.contains("type")) {
                record.setCategory(strValue);
            }
            
            if (lowerKey.contains("account")) {
                record.setAccount(strValue);
            }
        });
        
        return record;
    }
    
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        
        // Try common date formats
        DateTimeFormatter[] formatters = {
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("MM-dd-yyyy")
        };
        
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(dateStr.trim(), formatter);
            } catch (Exception e) {
                // Try next format
            }
        }
        
        throw new IllegalArgumentException("Unable to parse date: " + dateStr);
    }
    
    private BigDecimal parseAmount(String amountStr) {
        if (amountStr == null || amountStr.isEmpty()) {
            return null;
        }
        
        // Remove currency symbols and whitespace
        String cleaned = amountStr.replaceAll("[^\\d.-]", "").trim();
        
        if (cleaned.isEmpty()) {
            return null;
        }
        
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Unable to parse amount: " + amountStr);
        }
    }
}
