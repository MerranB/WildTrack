package com.wildtrack.exception;

public class EmailDeliveryException extends RuntimeException {

    public EmailDeliveryException(String message, Exception e){
        super(message + " " + e);
    }
}
