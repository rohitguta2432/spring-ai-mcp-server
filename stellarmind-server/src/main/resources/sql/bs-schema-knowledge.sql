-- ensure schema exists
CREATE SCHEMA IF NOT EXISTS bs AUTHORIZATION postgres;

-- =======================
-- Table: profile
-- =======================
CREATE TABLE bs.profile
(
    profile_name         VARCHAR(50) NOT NULL PRIMARY KEY,
    active               BOOLEAN     NOT NULL,
    creation_date        TIMESTAMP(6),
    description          VARCHAR(300),
    profile_hash         VARCHAR(64),
    profile_object       JSONB,
    update_date          TIMESTAMP(6),
    profile_cache_object JSONB
);

ALTER TABLE bs.profile OWNER TO postgres;

-- =======================
-- Table: event
-- =======================
CREATE TABLE bs.event
(
    event_id      SERIAL PRIMARY KEY,
    event_date    TIMESTAMP(6) NOT NULL,
    profile_name  VARCHAR(50),
    status        VARCHAR(50),
    serial_number VARCHAR(25),
    error         TEXT
);

ALTER TABLE bs.event OWNER TO postgres;

CREATE INDEX idx_event_serial_number
    ON bs.event (serial_number);

-- =======================
-- Table: flyway_schema_history
-- =======================
CREATE TABLE bs.flyway_schema_history
(
    installed_rank INTEGER                 NOT NULL PRIMARY KEY,
    version        VARCHAR(50),
    description    VARCHAR(200)            NOT NULL,
    type           VARCHAR(20)             NOT NULL,
    script         VARCHAR(1000)           NOT NULL,
    checksum       INTEGER,
    installed_by   VARCHAR(100)            NOT NULL,
    installed_on   TIMESTAMP DEFAULT now() NOT NULL,
    execution_time INTEGER                 NOT NULL,
    success        BOOLEAN                 NOT NULL
);

ALTER TABLE bs.flyway_schema_history OWNER TO postgres;

CREATE INDEX flyway_schema_history_s_idx
    ON bs.flyway_schema_history (success);

-- =======================
-- Table: bs_vehicle
-- =======================
CREATE TABLE bs.bs_vehicle
(
    vin                  VARCHAR(17)          NOT NULL PRIMARY KEY,
    active               BOOLEAN DEFAULT true NOT NULL,
    creation_date        TIMESTAMP(6)         NOT NULL,
    update_date          TIMESTAMP(6)         NOT NULL,
    vehicle_architecture VARCHAR(7)           NOT NULL,
    vehicle_program_code VARCHAR(7)           NOT NULL,
    vehicle_region       VARCHAR(7)           NOT NULL,
    profile_name         VARCHAR(50)
        REFERENCES bs.profile (profile_name)
);

ALTER TABLE bs.bs_vehicle OWNER TO postgres;

-- =======================
-- Table: bs_ecu
-- =======================
CREATE TABLE bs.bs_ecu
(
    serial_number       VARCHAR(25)                            NOT NULL PRIMARY KEY,
    active              BOOLEAN      DEFAULT true              NOT NULL,
    device_type         VARCHAR(10)                            NOT NULL,
    device_variant_type VARCHAR(50)                            NOT NULL,
    vin                 VARCHAR(17) REFERENCES bs.bs_vehicle(vin),
    creation_date       TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_date         TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP NOT NULL
);

ALTER TABLE bs.bs_ecu OWNER TO postgres;
