COMMENT ON DATABASE current_database() IS NULL;

SET search_path = public, pg_catalog;

COMMENT ON COLUMN typ_composite.key IS NULL;

COMMENT ON TYPE typ_composite IS NULL;

COMMENT ON DOMAIN dom IS NULL;

COMMENT ON SEQUENCE test_id_seq IS NULL;

COMMENT ON FUNCTION test_fnc(arg character varying) IS NULL;

COMMENT ON TABLE test IS NULL;

COMMENT ON COLUMN test.id IS NULL;

COMMENT ON COLUMN test.text IS NULL;

COMMENT ON CONSTRAINT text_check ON test IS NULL;

COMMENT ON CONSTRAINT test_pkey ON test IS NULL;

COMMENT ON VIEW test_view IS NULL;

COMMENT ON COLUMN test_view.id IS NULL;

COMMENT ON TRIGGER test_trigger ON test IS NULL;