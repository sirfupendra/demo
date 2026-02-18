package com.example.demo.exception;

/**
 * Custom exception for file processing errors
 */
public class FileProcessingException extends RuntimeException {
    public FileProcessingException(String message) {
        super(message);
    }

    public FileProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
