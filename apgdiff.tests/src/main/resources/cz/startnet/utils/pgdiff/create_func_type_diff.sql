SET search_path = public, pg_catalog;

CREATE TYPE typ_composite AS (
	key character varying(80) COLLATE pg_catalog."ru_RU.utf8",
	val text COLLATE pg_catalog."en_GB"
);

ALTER TYPE typ_composite OWNER TO botov_av;

CREATE OR REPLACE FUNCTION add(typ_composite, integer) RETURNS integer
    LANGUAGE sql IMMUTABLE STRICT
    AS $_$select $2;$_$;

ALTER FUNCTION add(typ_composite, integer) OWNER TO botov_av;
