SET search_path = public, pg_catalog;

ALTER TABLE t1
	ADD COLUMN c2 text;

-- COLUMN c2 GRANT

GRANT ALL(c2) ON TABLE t1 TO maindb;

-- COLUMN c1 GRANT

REVOKE ALL(c1) ON TABLE t1 FROM PUBLIC;
REVOKE ALL(c1) ON TABLE t1 FROM botov_av;
GRANT ALL(c1) ON TABLE t1 TO maindb;
