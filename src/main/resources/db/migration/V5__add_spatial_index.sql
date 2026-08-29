  CREATE INDEX IF NOT EXISTS idx_movebank_event_location
      ON movebank_event
      USING GIST (location);