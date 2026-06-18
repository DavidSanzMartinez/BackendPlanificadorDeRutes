CREATE TABLE stops (
    stop_id VARCHAR(255) PRIMARY KEY,
    stop_name VARCHAR(255) NOT NULL,
    stop_lat DOUBLE PRECISION NOT NULL,
    stop_lon DOUBLE PRECISION NOT NULL,
    wheelchair_boarding INTEGER DEFAULT 0
);

CREATE TABLE routes (
    route_id VARCHAR(255) PRIMARY KEY,
    route_short_name VARCHAR(255),
    route_long_name VARCHAR(255),
    route_type INTEGER NOT NULL,
    route_color VARCHAR(10),
    route_text_color VARCHAR(10)
);

CREATE TABLE calendar (
    service_id VARCHAR(255) PRIMARY KEY,
    monday BOOLEAN NOT NULL,
    tuesday BOOLEAN NOT NULL,
    wednesday BOOLEAN NOT NULL,
    thursday BOOLEAN NOT NULL,
    friday BOOLEAN NOT NULL,
    saturday BOOLEAN NOT NULL,
    sunday BOOLEAN NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL
);

CREATE TABLE trips (
    trip_id VARCHAR(255) PRIMARY KEY,
    route_id VARCHAR(255) NOT NULL REFERENCES routes(route_id),
    service_id VARCHAR(255) NOT NULL REFERENCES calendar(service_id),
    trip_headsign VARCHAR(255),
    wheelchair_accessible INTEGER DEFAULT 0,
    block_id VARCHAR(255)
);

CREATE TABLE stop_times (
    trip_id VARCHAR(255) NOT NULL REFERENCES trips(trip_id),
    stop_sequence INTEGER NOT NULL,
    stop_id VARCHAR(255) NOT NULL REFERENCES stops(stop_id),
    arrival_time VARCHAR(8),
    departure_time VARCHAR(8),
    PRIMARY KEY ( trip_id, stop_sequence)
);


CREATE INDEX idx_stop_times_trip_seq
    ON stop_times (trip_id, stop_sequence);

CREATE INDEX idx_stop_times_stop_departure
    ON stop_times (stop_id, departure_time);

CREATE INDEX idx_trips_service_id
    ON trips (service_id);