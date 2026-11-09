package com.turkcell.libraryapp.memberservice.exception;

/** Kaynak sahipliği / rol ihlali → HTTP 403. */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
