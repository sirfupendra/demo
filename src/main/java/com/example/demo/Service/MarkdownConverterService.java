package com.example.demo.service;

import com.example.demo.dto.FinancialDataRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for converting financial data to markdown format
 */
@Slf4j
@Service
public class MarkdownConverterService {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    public String convertToMarkdown(List<FinancialDataRecord> records, String filename) {
        if (records == null || records.isEmpty()) {
            return generateEmptyMarkdown(filename);
        }
        
        StringBuilder markdown = new StringBuilder();
        
        // Header
        markdown.append("# Financial Data Report\n\n");
        markdown.append(String.format("**Source File:** %s\n\n", filename));
        markdown.append(String.format("**Total Records:** %d\n\n", records.size()));
        markdown.append("---\n\n");
        
        // Summary section
        markdown.append("## Summary\n\n");
        markdown.append(generateSummary(records));
        markdown.append("\n---\n\n");
        
        // Data table
        markdown.append("## Financial Records\n\n");
        markdown.append(generateTable(records));
        markdown.append("\n");
        
        // Detailed records section
        markdown.append("## Detailed Records\n\n");
        for (int i = 0; i < records.size(); i++) {
            markdown.append(generateRecordDetails(records.get(i), i + 1));
            markdown.append("\n");
        }
        
        log.info("Converted {} records to markdown format", records.size());
        return markdown.toString();
    }
    
    private String generateEmptyMarkdown(String filename) {
        StringBuilder markdown = new StringBuilder();
        markdown.append("# Financial Data Report\n\n");
        markdown.append(String.format("**Source File:** %s\n\n", filename));
        markdown.append("**Status:** No records found in the file.\n\n");
        return markdown.toString();
    }
    
    private String generateSummary(List<FinancialDataRecord> records) {
        StringBuilder summary = new StringBuilder();
        
        // Calculate totals
        BigDecimal totalAmount = records.stream()
            .filter(r -> r.getAmount() != null)
            .map(FinancialDataRecord::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        long recordsWithAmount = records.stream()
            .filter(r -> r.getAmount() != null)
            .count();
        
        long recordsWithDate = records.stream()
            .filter(r -> r.getDate() != null)
            .count();
        
        summary.append("| Metric | Value |\n");
        summary.append("|--------|-------|\n");
        summary.append(String.format("| Total Records | %d |\n", records.size()));
        summary.append(String.format("| Records with Amount | %d |\n", recordsWithAmount));
        summary.append(String.format("| Records with Date | %d |\n", recordsWithDate));
        
        if (totalAmount.compareTo(BigDecimal.ZERO) != 0) {
            summary.append(String.format("| Total Amount | %s |\n", formatCurrency(totalAmount)));
            BigDecimal average = totalAmount.divide(BigDecimal.valueOf(recordsWithAmount), 2, 
                java.math.RoundingMode.HALF_UP);
            summary.append(String.format("| Average Amount | %s |\n", formatCurrency(average)));
        }
        
        // Category breakdown
        Map<String, Long> categoryCount = records.stream()
            .filter(r -> r.getCategory() != null && !r.getCategory().isEmpty())
            .collect(Collectors.groupingBy(FinancialDataRecord::getCategory, Collectors.counting()));
        
        if (!categoryCount.isEmpty()) {
            summary.append("\n### Categories\n\n");
            summary.append("| Category | Count |\n");
            summary.append("|----------|-------|\n");
            categoryCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> summary.append(String.format("| %s | %d |\n", 
                    entry.getKey(), entry.getValue())));
        }
        
        return summary.toString();
    }
    
    private String generateTable(List<FinancialDataRecord> records) {
        if (records.isEmpty()) {
            return "No records available.";
        }
        
        StringBuilder table = new StringBuilder();
        
        // Get all unique field names from all records
        Set<String> allFields = records.stream()
            .flatMap(r -> r.getFields().keySet().stream())
            .collect(java.util.stream.Collectors.toSet());
        
        List<String> fieldList = allFields.stream()
            .sorted()
            .collect(java.util.stream.Collectors.toList());
        
        // Limit to most common fields for table display
        List<String> displayFields = fieldList.stream()
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        
        // Add standard financial fields if they exist
        if (!displayFields.contains("Date") && fieldList.stream().anyMatch(f -> f.toLowerCase().contains("date"))) {
            displayFields.add(0, fieldList.stream().filter(f -> f.toLowerCase().contains("date")).findFirst().orElse(""));
        }
        if (!displayFields.contains("Amount") && fieldList.stream().anyMatch(f -> f.toLowerCase().contains("amount"))) {
            displayFields.add(fieldList.stream().filter(f -> f.toLowerCase().contains("amount")).findFirst().orElse(""));
        }
        
        // Table header
        table.append("| # | ");
        for (String field : displayFields) {
            table.append(escapeMarkdown(field)).append(" | ");
        }
        table.append("\n");
        
        // Table separator
        table.append("|");
        for (int i = 0; i <= displayFields.size(); i++) {
            table.append("---|");
        }
        table.append("\n");
        
        // Table rows
        for (int i = 0; i < records.size(); i++) {
            FinancialDataRecord record = records.get(i);
            table.append("| ").append(i + 1).append(" | ");
            
            for (String field : displayFields) {
                Object value = record.getFields().get(field);
                String displayValue = value != null ? value.toString() : "";
                
                // Format special fields
                if (field.toLowerCase().contains("date") && record.getDate() != null) {
                    displayValue = record.getDate().format(DATE_FORMATTER);
                } else if (field.toLowerCase().contains("amount") && record.getAmount() != null) {
                    displayValue = formatCurrency(record.getAmount());
                }
                
                table.append(escapeMarkdown(displayValue)).append(" | ");
            }
            table.append("\n");
        }
        
        return table.toString();
    }
    
    private String generateRecordDetails(FinancialDataRecord record, int index) {
        StringBuilder details = new StringBuilder();
        
        details.append(String.format("### Record #%d\n\n", index));
        
        // Standard fields
        if (record.getDate() != null) {
            details.append(String.format("- **Date:** %s\n", record.getDate().format(DATE_FORMATTER)));
        }
        
        if (record.getAmount() != null) {
            details.append(String.format("- **Amount:** %s\n", formatCurrency(record.getAmount())));
        }
        
        if (record.getDescription() != null && !record.getDescription().isEmpty()) {
            details.append(String.format("- **Description:** %s\n", escapeMarkdown(record.getDescription())));
        }
        
        if (record.getCategory() != null && !record.getCategory().isEmpty()) {
            details.append(String.format("- **Category:** %s\n", escapeMarkdown(record.getCategory())));
        }
        
        if (record.getAccount() != null && !record.getAccount().isEmpty()) {
            details.append(String.format("- **Account:** %s\n", escapeMarkdown(record.getAccount())));
        }
        
        // All fields
        if (!record.getFields().isEmpty()) {
            details.append("\n**All Fields:**\n\n");
            details.append("| Field | Value |\n");
            details.append("|-------|-------|\n");
            
            record.getFields().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String value = entry.getValue() != null ? entry.getValue().toString() : "";
                    details.append(String.format("| %s | %s |\n", 
                        escapeMarkdown(entry.getKey()), escapeMarkdown(value)));
                });
        }
        
        return details.toString();
    }
    
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "N/A";
        }
        return String.format("$%,.2f", amount);
    }
    
    private String escapeMarkdown(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("|", "\\|")
                  .replace("\n", " ")
                  .replace("\r", " ");
    }
}
