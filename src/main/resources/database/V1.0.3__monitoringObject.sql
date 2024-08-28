create table monitoring_object (
    fdn varchar NOT NULL,
    pme_session_id varchar(255),
    state varchar(7) NOT NULL,
    start_time TIMESTAMP,
    last_processed_time TIMESTAMP,
    end_time TIMESTAMP,
    PRIMARY KEY(fdn, pme_session_id)
);