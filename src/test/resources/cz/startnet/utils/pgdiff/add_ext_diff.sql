
CREATE SCHEMA other;

DROP EXTENSION testext1;

CREATE EXTENSION testext3 SCHEMA other VERSION 2;

ALTER EXTENSION testext2 SET SCHEMA other;
