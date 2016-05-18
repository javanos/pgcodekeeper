-- extension owned triggers are skipped by rel != null check in java code

SELECT  ccc.relname, 
		r.rulename, 
		r.ev_type, 
		r.ev_enabled, 
		r.is_instead, 
		pg_get_ruledef(r.oid) AS rule_string
FROM pg_catalog.pg_rewrite r
JOIN pg_catalog.pg_class ccc ON ccc.oid = r.ev_class 
WHERE ccc.relnamespace = ? AND
    -- block rules that implement views
    NOT ((ccc.relkind = 'v' OR ccc.relkind = 'm') AND r.ev_type = '1' AND r.is_instead)