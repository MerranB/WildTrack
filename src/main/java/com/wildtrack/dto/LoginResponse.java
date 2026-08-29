package com.wildtrack.dto;

public record LoginResponse(String token, long expiresInSeconds) {

}