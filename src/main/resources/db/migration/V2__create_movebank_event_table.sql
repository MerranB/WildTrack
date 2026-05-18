CREATE TABLE IF NOT EXISTS movebank_event (
    id         BIGSERIAL    PRIMARY KEY,
    timestamp  TIMESTAMP,
    location geometry(Point, 4326),
    individual_id TEXT,
    tag_id TEXT,
     CONSTRAINT unique_data_point UNIQUE (timestamp, location, individual_id, tag_id)
);