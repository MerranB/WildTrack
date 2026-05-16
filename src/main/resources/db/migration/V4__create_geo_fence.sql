CREATE TABLE IF NOT EXISTS geo_fence (
    id    BIGSERIAL  PRIMARY KEY,
    name  TEXT,
    area geometry(Polygon, 4326),
    email TEXT,
    username TEXT
);