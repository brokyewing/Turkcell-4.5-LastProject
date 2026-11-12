package com.turkcell.libraryapp.memberservice.exception;

import com.turkcell.libraryapp.memberservice.exception.detail.ExceptionDetails;
import com.turkcell.libraryapp.memberservice.exception.detail.ValidationExceptionDetails;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Set;
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

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ExceptionDetails> handleForbiddenException(ForbiddenException e) {
        ExceptionDetails details = new ExceptionDetails(e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(details);
    }
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ExceptionDetails> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        Set<HttpMethod> supported = e.getSupportedHttpMethods();
        String message = "Method " + e.getMethod() + " not allowed";
        HttpHeaders headers = new HttpHeaders();
        if (supported != null && !supported.isEmpty()) {
            message += ". Supported: " + supported.stream().map(HttpMethod::name).collect(Collectors.joining(", "));
            headers.setAllow(supported);
        }
        ExceptionDetails details = new ExceptionDetails(message);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).headers(headers).body(details);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ExceptionDetails> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        ExceptionDetails details = new ExceptionDetails("Media type " + e.getContentType() + " not supported");
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(details);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionDetails> handleGenericException(Exception e) {
        ExceptionDetails details = new ExceptionDetails("An error occurred: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(details);
    }
}







