ALTER TABLE gtfs_metadata ALTER COLUMN id TYPE BIGINT;
ALTER TABLE gtfs_metadata ALTER COLUMN id SET DEFAULT nextval('gtfs_metadata_id_seq');