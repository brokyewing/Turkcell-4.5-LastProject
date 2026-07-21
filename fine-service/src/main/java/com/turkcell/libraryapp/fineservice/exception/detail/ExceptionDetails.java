package com.turkcell.libraryapp.fineservice.exception.detail;

public class ExceptionDetails {
    private String message;
    private String timestamp;
    
    public ExceptionDetails(String message) {
        this.message = message;
        this.timestamp = java.time.LocalDateTime.now().toString();
    }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}







