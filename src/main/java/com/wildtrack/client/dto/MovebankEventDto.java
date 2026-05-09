package com.wildtrack.client.dto;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvDate;
import com.opencsv.bean.CsvIgnore;
import java.time.LocalDateTime;

public class MovebankEventDto {

    @CsvIgnore
    Long id;
    @CsvDate("yyyy-MM-dd HH:mm:ss.SSS")
    @CsvBindByName(column = "timestamp")
    private LocalDateTime timestamp;
    @CsvBindByName(column = "location_lat")
    private Double locationLat;
    @CsvBindByName(column = "location_long")
    private Double locationLong;
    @CsvBindByName(column = "individual_id")
    private String individualId;
    @CsvBindByName(column = "tag_id")
    private String tagId;

    public MovebankEventDto (){}


    public MovebankEventDto (LocalDateTime timestamp, Double locationLat,
                             Double locationLong, String individualId, String tagId) {
        this.timestamp = timestamp;
        this.locationLat = locationLat;
        this.locationLong = locationLong;
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

    public Double getLocationLat() {
        return locationLat;
    }

    public void setLocationLat(Double locationLat) {
        this.locationLat = locationLat;
    }

    public Double getLocationLong() {
        return locationLong;
    }

    public void setLocationLong(Double locationLong) {
        this.locationLong = locationLong;
    }

    public String getIndividualId() {
        return individualId;
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