package com.wildtrack.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.locationtech.jts.geom.Polygon;
import java.time.LocalDateTime;

@Entity
@Table(name = "geo_fence")
@Getter
@Setter
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

    @PrePersist
    private void prePersist() {
        if (lastAlertSent == null) {
            lastAlertSent = LocalDateTime.now();
        }
    }
}