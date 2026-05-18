package com.wildtrack.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.time.LocalDateTime;

public class GeoFenceDto {

    private Long id;
    
    @NotBlank
    private String name;

    private List<CoordinateDto> coordinates;

    @Email
    private String email;

    private String username;

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


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }

    public void setCoordinates(List<CoordinateDto> coordinates) {
        this.coordinates = coordinates;
    }

    public List<CoordinateDto> getCoordinates() {
        return coordinates;
    }

    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getEmail() {
        return email;
    }

    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getUsername() {
        return username;
    }

    public void setLastAnimalCount(int lastAnimalCount) {
        this.lastAnimalCount = lastAnimalCount;
    }
    
    public int getLastAnimalCount() {
        return lastAnimalCount;
    }

    public void setLastLastAlertSent(LocalDateTime lastAlertSent) {
        this.lastAlertSent = lastAlertSent;
    }
    
    public LocalDateTime getLastAlertSent() {
        return lastAlertSent;
    }

}