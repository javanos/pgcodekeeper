SET search_path = public, pg_catalog;

ALTER DOMAIN dom5
	ADD CONSTRAINT dom5_check2 CHECK ((VALUE <> 1)) NOT VALID;
