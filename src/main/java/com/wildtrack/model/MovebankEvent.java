package com.wildtrack.model;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "movebank_event")
@NoArgsConstructor
public class MovebankEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @Column(name = "location", columnDefinition = "geometry(Point, 4326)")
    private Point location;

    @Column(name = "individual_id")
    private String individualId;

    @Column(name = "tag_id")
    private String tagId;

    public MovebankEvent(LocalDateTime timestamp, Point location, String individualId, String tagId) {
        this.timestamp = timestamp;
        this.location = location;
        this.individualId = individualId;
        this.tagId = tagId;
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

    public String getIndividualId() {
        return individualId;
    }

    public void setLocation(Point location) {
        this.location = location;
    }


    public Point getLocation() {
        return location;
    }

    public void setIndividualId(String individualId) {
        this.individualId = individualId;
    }

    public String getTagId() {
        return tagId;
    }

    public void setTagId(String tagId) {
        this.tagId = tagId;
    }
}