package com.wildtrack.exception;

public class MovebankRateLimitException extends RuntimeException {

    public MovebankRateLimitException(){
        super("Daily call limit reached for Movebank API.");
    }
}
