package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for markdown conversion
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkdownResponse {
    private String markdown;
    private String filename;
    private int recordCount;
    private LocalDateTime processedAt;
    private String fileType;
    private String status;
}
