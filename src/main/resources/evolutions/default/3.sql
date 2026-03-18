-- !Ups
CREATE INDEX telemetry_read_at_idx ON telemetry (read_at);

-- !Downs

DROP INDEX telemetry_read_at_idx;