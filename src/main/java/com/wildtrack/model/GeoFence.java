package com.wildtrack.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Polygon;
import java.time.LocalDateTime;

@Entity
@Table(name = "geo_fence")
@NoArgsConstructor
public class GeoFence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "area", columnDefinition = "geometry(Polygon, 4326)")
    private Polygon area;

    @Column(name = "email")
    private String email;

    @Column(name = "username")
    private String username;

    @Min(0)
    @NotNull
    @Column(name = "last_animal_count")
    private int lastAnimalCount;

    @NotNull
    @Column(name = "last_alert_sent")
    private LocalDateTime lastAlertSent;

    public GeoFence(String name, Polygon area, String email, String username, int lastAnimalCount) {
        this.name = name;
        this.area = area;
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

    public void setArea(Polygon area) {
        this.area = area;
    }

    public Polygon getArea() {
        return area;
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

    public void setLastAlertSent(LocalDateTime lastAlertSent) {
        this.lastAlertSent = lastAlertSent;
    }
    
    public LocalDateTime getLastAlertSent() {
        return lastAlertSent;
    }
}