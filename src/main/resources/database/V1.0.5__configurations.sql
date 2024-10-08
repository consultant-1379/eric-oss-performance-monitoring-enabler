DROP table kpi_configurations;

create table kpi_configurations (
    id bigint generated by default as identity primary key,
    kpi_name varchar(255) NOT NULL,
    monitor boolean NOT NULL,
    threshold_type varchar(8) NOT NULL,
    fixed_threshold_value double precision,
    relevance_threshold double precision,
    confidence_interval double precision,
    session_configuration_id bigint
);
