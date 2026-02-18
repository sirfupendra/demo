package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Information about a file extracted from a ZIP archive
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZipFileInfo {
    private String filename;
    private String fileType;
    private int recordCount;
    private boolean processed;
    private String errorMessage;
}
