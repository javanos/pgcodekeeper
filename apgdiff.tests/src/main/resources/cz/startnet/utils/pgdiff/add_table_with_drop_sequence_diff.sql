SET search_path = public, pg_catalog;

DROP SEQUENCE testtable2_sequence_seq;

CREATE TABLE testtable2 (
	id integer,
	name character varying(100) NOT NULL,
	sequence integer NOT NULL
);

ALTER TABLE testtable2 OWNER TO fordfrog;
