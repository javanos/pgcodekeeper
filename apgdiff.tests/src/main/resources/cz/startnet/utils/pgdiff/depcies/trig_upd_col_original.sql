--
-- PostgreSQL database dump
--

SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

--CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

--COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET search_path = public, pg_catalog;

--
-- Name: incr(); Type: FUNCTION; Schema: public; Owner: botov_av
--

CREATE FUNCTION incr() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
                RETURN 1;
        END;
$$;


ALTER FUNCTION public.incr() OWNER TO botov_av;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: t1; Type: TABLE; Schema: public; Owner: botov_av; Tablespace: 
--

CREATE TABLE t1 (
    c1 integer,
    c2 integer,
    c3 integer
);


ALTER TABLE public.t1 OWNER TO botov_av;

--
-- Name: trig1; Type: TRIGGER; Schema: public; Owner: botov_av
--

CREATE TRIGGER trig1 AFTER UPDATE OF c1, c2 ON t1 FOR EACH ROW EXECUTE PROCEDURE incr();


--
-- Name: public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- PostgreSQL database dump complete
--

