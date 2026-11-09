package com.turkcell.libraryapp.fineservice.exception;

/** Kaynak sahipligi / rol ihlali -> HTTP 403. */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
