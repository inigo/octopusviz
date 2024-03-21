-- !Ups
CREATE TABLE telemetry (
                             id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
                             read_at TIMESTAMP NOT NULL,
                             consumption_delta DOUBLE PRECISION NOT NULL,
                             demand DOUBLE PRECISION NOT NULL
);

-- !Downs

DROP TABLE telemetry;