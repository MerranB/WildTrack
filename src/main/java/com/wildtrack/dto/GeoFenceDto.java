package com.wildtrack.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class GeoFenceDto {

    private Long id;
    
    @NotBlank
    private String name;

    @NotNull @Size(min = 4)
    private List<CoordinateDto> coordinates;

    @Email
    private String email;

    private String username;

    @Min(0)
    private int lastAnimalCount;

    private LocalDateTime lastAlertSent;

    public GeoFenceDto(String name, List<CoordinateDto> coordinates, String email, String username,
                       int lastAnimalCount) {
        this.name = name;
        this.coordinates = coordinates;
        this.email = email;
        this.username = username;
        this.lastAnimalCount = lastAnimalCount;
        this.lastAlertSent = LocalDateTime.now();
        }
}