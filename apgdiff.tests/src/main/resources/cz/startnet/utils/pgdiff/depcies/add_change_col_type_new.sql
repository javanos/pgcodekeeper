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

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: t1; Type: TABLE; Schema: public; Owner: botov_av; Tablespace: 
--

CREATE TABLE t1 (
    c1 text
);


ALTER TABLE public.t1 OWNER TO botov_av;

--
-- Name: t2; Type: TABLE; Schema: public; Owner: botov_av; Tablespace: 
--

CREATE TABLE t2 (
    c2 text
);


ALTER TABLE public.t2 OWNER TO botov_av;

--
-- Name: v1; Type: VIEW; Schema: public; Owner: botov_av
--

CREATE VIEW v1 AS
 SELECT t1.c1,
    t2.c2,
    'asdad' AS text
   FROM t1,
    t2;


ALTER TABLE public.v1 OWNER TO botov_av;

--
-- Name: VIEW v1; Type: COMMENT; Schema: public; Owner: botov_av
--

COMMENT ON VIEW v1 IS 'asdsada';

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

