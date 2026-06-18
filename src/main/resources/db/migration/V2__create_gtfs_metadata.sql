CREATE TABLE gtfs_metadata (
   id          SERIAL PRIMARY KEY,
   file_name   VARCHAR(255) NOT NULL UNIQUE,
   checksum    VARCHAR(32) NOT NULL,
   loaded_at   TIMESTAMP NOT NULL
);