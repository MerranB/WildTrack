package com.wildtrack.client.dto;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvDate;
import com.opencsv.bean.CsvIgnore;
import java.time.LocalDateTime;

public class MovebankEventDto {

    @CsvIgnore
    int id;
    @CsvDate("yyyy-MM-dd HH:mm:ss.SSS")
    @CsvBindByName(column = "timestamp")
    private LocalDateTime timestamp;
    @CsvBindByName(column = "location_lat")
    private Double location_lat;
    @CsvBindByName(column = "location_long")
    private Double location_long;
    @CsvBindByName(column = "individual_id")
    private String individual_id;
    @CsvBindByName(column = "tag_id")
    private String tag_id;

    public MovebankEventDto (){}


    public MovebankEventDto (LocalDateTime timestamp, Double location_lat,
                             Double location_long, String individual_id, String tag_id) {
        this.timestamp = timestamp;
        this.location_lat = location_lat;
        this.location_long = location_long;
        this.individual_id = individual_id;
        this.tag_id = tag_id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
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