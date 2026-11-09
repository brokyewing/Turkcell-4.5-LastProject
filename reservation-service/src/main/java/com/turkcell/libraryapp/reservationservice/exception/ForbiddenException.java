package com.turkcell.libraryapp.reservationservice.exception;

/** Kaynak sahipligi / rol ihlali -> HTTP 403. */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
