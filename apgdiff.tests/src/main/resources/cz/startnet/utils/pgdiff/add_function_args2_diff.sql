SET search_path = public, pg_catalog;

CREATE OR REPLACE FUNCTION multiply_numbers(number1 integer, number2 integer) RETURNS integer
    AS $$
begin
	return number1 * number2;
end;
$$
    LANGUAGE plpgsql;

ALTER FUNCTION multiply_numbers(number1 integer, number2 integer) OWNER TO fordfrog;
