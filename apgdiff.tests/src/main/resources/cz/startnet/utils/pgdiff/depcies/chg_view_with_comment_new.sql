--
-- PostgreSQL database dump
--

-- Dumped from database version 9.6.3
-- Dumped by pg_dump version 9.6.3

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

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
-- Name: testtable; Type: TABLE; Schema: public; Owner: galiev_mr
--

CREATE TABLE testtable (
    c1 integer,
    c2 text
);


ALTER TABLE testtable OWNER TO galiev_mr;

--
-- Name: testview; Type: VIEW; Schema: public; Owner: galiev_mr
--

CREATE VIEW testview AS
 SELECT testtable.c1,
    testtable.c2
   FROM testtable;


ALTER TABLE testview OWNER TO galiev_mr;

--
-- Name: VIEW testview; Type: COMMENT; Schema: public; Owner: galiev_mr
--

COMMENT ON VIEW testview IS 'this is test comment';


--
-- PostgreSQL database dump complete
--

