-- !Ups

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE consumption (
                             id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
                             interval_start TIMESTAMP NOT NULL,
                             interval_end TIMESTAMP NOT NULL,
                             consumption DOUBLE PRECISION NOT NULL,
                             energy_type text not null 
);

-- !Downs

DROP TABLE consumption;