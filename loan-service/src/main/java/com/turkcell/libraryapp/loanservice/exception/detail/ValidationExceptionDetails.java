package com.turkcell.libraryapp.loanservice.exception.detail;

import java.util.List;

public class ValidationExceptionDetails {
    private String title;
    private List<Object> errors;
    
    public ValidationExceptionDetails(String title, List<Object> errors) {
        this.title = title;
        this.errors = errors;
    }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public List<Object> getErrors() { return errors; }
    public void setErrors(List<Object> errors) { this.errors = errors; }
}



