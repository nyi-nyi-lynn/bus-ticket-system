package com.busticket.exception;

public class DuplicateResourceException extends Exception {
    private static final long serialVersionUID = 1L;

    public DuplicateResourceException(String message) {
        super(message);
    }
}
