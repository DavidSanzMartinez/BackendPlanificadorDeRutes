CREATE TABLE footpaths (
                           from_stop_id    VARCHAR(255) NOT NULL,
                           to_stop_id      VARCHAR(255) NOT NULL,
                           transfer_time   INTEGER NOT NULL,
                           distance_meters DOUBLE PRECISION,
                           PRIMARY KEY (from_stop_id, to_stop_id)
);

CREATE INDEX idx_footpaths_from_stop ON footpaths (from_stop_id);