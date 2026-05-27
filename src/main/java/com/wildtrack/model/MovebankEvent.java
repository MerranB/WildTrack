package com.wildtrack.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

import lombok.Setter;
import org.locationtech.jts.geom.Point;

@Entity
@Getter
@Setter
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
}