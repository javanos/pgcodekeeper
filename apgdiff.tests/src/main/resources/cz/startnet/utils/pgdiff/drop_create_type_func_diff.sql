SET search_path = public, pg_catalog;

-- DEPCY: This FUNCTION depends on the TYPE: typ_range

DROP FUNCTION add(typ_range, integer);

DROP TYPE typ_range;

-- DEPCY: This TYPE is a dependency of FUNCTION: add(typ_range, integer)

CREATE TYPE typ_range AS RANGE (
	subtype = character varying,
	collation = pg_catalog."ru_RU"
);

ALTER TYPE typ_range OWNER TO botov_av;

CREATE OR REPLACE FUNCTION add(typ_range, integer) RETURNS integer
    LANGUAGE sql IMMUTABLE STRICT
    AS $_$select $2;$_$;

ALTER FUNCTION add(typ_range, integer) OWNER TO botov_av;
