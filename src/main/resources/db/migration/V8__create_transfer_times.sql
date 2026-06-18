CREATE TABLE transfer_times (
                                from_stop_id    VARCHAR(255) NOT NULL,
                                to_stop_id      VARCHAR(255) NOT NULL,
                                from_route_id   VARCHAR(255),
                                to_route_id     VARCHAR(255),
                                transfer_type   INTEGER NOT NULL DEFAULT 2,
                                min_transfer_time INTEGER NOT NULL,
                                PRIMARY KEY (from_stop_id, to_stop_id, from_route_id, to_route_id)
);

CREATE INDEX idx_transfer_times_from ON transfer_times (from_stop_id, from_route_id);