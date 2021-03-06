SET search_path = public, pg_catalog;

-- DEPCY: This VIEW depends on the COLUMN: t1.c1

DROP VIEW v1;

ALTER TABLE t1
	ALTER COLUMN c1 TYPE bigInt USING c1::bigInt; /* TYPE change - table: t1 original: integer new: bigInt */

ALTER TABLE t2
	ALTER COLUMN c4 TYPE bigInt USING c4::bigInt; /* TYPE change - table: t2 original: integer new: bigInt */

CREATE VIEW v1 AS
	select * from t1, (select * from t2) q1;
