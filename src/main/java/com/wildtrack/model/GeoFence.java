package com.wildtrack.model;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Polygon;

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

    public GeoFence(String name, Polygon area, String email, String username) {
        this.name = name;
        this.area = area;
        this.email = email;
        this.username = username;
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
}