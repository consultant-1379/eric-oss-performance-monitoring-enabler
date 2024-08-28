create table sessions (
    id varchar(255) PRIMARY KEY,
    client_id varchar(255) NOT NULL,
    session_reference varchar(255) NOT NULL,
    duration integer NOT NULL,
    session_configuration_id bigint NOT NULL REFERENCES session_configurations,
    status varchar(8) NOT NULL,
    created_at timestamp(6) NOT NULL,
    started_at timestamp(6),
    finished_at timestamp(6),
    UNIQUE(client_id, session_reference)
);