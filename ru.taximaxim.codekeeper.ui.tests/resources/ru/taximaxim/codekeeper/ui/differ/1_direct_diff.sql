SET TIMEZONE TO 'UTC';

SET search_path = public, pg_catalog;

ALTER TABLE t4
	DROP CONSTRAINT t4_c2_key;

ALTER TABLE t4
	ADD CONSTRAINT t4_c2_key UNIQUE (c2);
