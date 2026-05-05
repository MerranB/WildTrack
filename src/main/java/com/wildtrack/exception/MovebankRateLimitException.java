package com.wildtrack.exception;

public class MovebankRateLimitException extends RuntimeException {

    public MovebankRateLimitException(){
        super("Movebank API call failed");
    }
}
