package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of processing a ZIP file containing multiple financial data files
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZipProcessingResult {
    private List<FinancialDataRecord> allRecords;
    private List<ZipFileInfo> fileInfos;
    private int totalFiles;
    private int successfullyProcessedFiles;
    
    public ZipProcessingResult() {
        this.allRecords = new ArrayList<>();
        this.fileInfos = new ArrayList<>();
        this.totalFiles = 0;
        this.successfullyProcessedFiles = 0;
    }
}
