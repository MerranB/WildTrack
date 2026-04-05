package com.wildtrack.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record ItemDto(
        Long id,

        @NotBlank(message = "Name must not be blank")
        String name,

        String description,

        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ItemDto of(String name, String description) {
        return new ItemDto(null, name, description, null, null);
    }
}
