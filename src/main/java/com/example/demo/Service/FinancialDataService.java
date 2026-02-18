package com.example.demo.service;

import com.example.demo.dto.FinancialDataRecord;
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

import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

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
        };
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
