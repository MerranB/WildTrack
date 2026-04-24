package com.wildtrack.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "MovebankEvent")
@Getter
@Setter
@NoArgsConstructor
public class MovebankEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @Column(name = "location_lat")
    private Double location_lat;

    @CreationTimestamp
    @Column(name = "location_long")
    private Double location_long;

    @UpdateTimestamp
    @Column(name = "individual_id")
    private String individual_id;

    @UpdateTimestamp
    @Column(name = "tag_id")
    private String tag_id;

    public MovebankEvent(LocalDateTime timestamp, Double location_lat,
                         Double location_long, String individual_id, String tag_id) {
        this.timestamp = timestamp;
        this.location_lat = location_lat;
        this.location_long = location_long;
        this.individual_id = individual_id;
        this.tag_id = tag_id;
        }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Double getLocation_lat() {
        return location_lat;
    }

    public void setLocation_lat(Double location_lat) {
        this.location_lat = location_lat;
    }

    public Double getLocation_long() {
        return location_long;
    }

    public void setLocation_long(Double location_long) {
        this.location_long = location_long;
    }

    public String getIndividual_id() {
        return individual_id;
    }

    public void setIndividual_id(String individual_id) {
        this.individual_id = individual_id;
    }

    public String getTag_id() {
        return tag_id;
    }

    public void setTag_id(String tag_id) {
        this.tag_id = tag_id;
    }
}