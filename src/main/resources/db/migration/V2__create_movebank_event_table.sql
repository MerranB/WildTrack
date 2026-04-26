CREATE TABLE IF NOT EXISTS movebank_event (
    id         BIGSERIAL    PRIMARY KEY,
    timestamp  TIMESTAMP,
    location_lat DOUBLE PRECISION,
    location_long DOUBLE PRECISION,
    individual_id TEXT,
    tag_id TEXT,
     CONSTRAINT unique_data_point UNIQUE (timestamp, location_lat,location_long, individual_id, tag_id)
);
