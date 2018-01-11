SET search_path = public, pg_catalog;

ALTER TABLE testtable
	ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
	SEQUENCE NAME custom_named_seq
	START WITH 1
	INCREMENT BY 2
	NO MAXVALUE
	NO MINVALUE
	CACHE 1
);
