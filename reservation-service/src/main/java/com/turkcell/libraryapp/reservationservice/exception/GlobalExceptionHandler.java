package com.turkcell.libraryapp.reservationservice.exception;

import com.turkcell.libraryapp.reservationservice.exception.detail.ExceptionDetails;
import com.turkcell.libraryapp.reservationservice.exception.detail.ValidationExceptionDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationExceptionDetails> handleValidationException(MethodArgumentNotValidException ex) {
        ValidationExceptionDetails details = new ValidationExceptionDetails(
                "Validation Failed",
                ex.getBindingResult().getAllErrors().stream()
                        .map(error -> error.getDefaultMessage())
                        .collect(Collectors.toList())
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(details);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ExceptionDetails> handleBusinessException(BusinessException e) {
        ExceptionDetails details = new ExceptionDetails(e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(details);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionDetails> handleGenericException(Exception e) {
        ExceptionDetails details = new ExceptionDetails("An error occurred: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(details);
    }
}



