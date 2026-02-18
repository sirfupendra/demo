package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Represents a single financial data record
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinancialDataRecord {
    private Map<String, Object> fields;
    private LocalDate date;
    private BigDecimal amount;
    private String description;
    private String category;
    private String account;
    
    public FinancialDataRecord(Map<String, Object> fields) {
        this.fields = fields;
    }
}
