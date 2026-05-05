package com.wildtrack.exception;

public class MovebankApiException extends RuntimeException {

    public MovebankApiException() {
        super("Movebank API call failed");
    }
}