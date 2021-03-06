--
-- PostgreSQL database dump
--

-- Dumped from database version 9.5.5
-- Dumped by pg_dump version 9.5.5

SET statement_timeout = 0;
SET lock_timeout = 0;
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

--
-- Name: testtype; Type: TYPE; Schema: public; Owner: shamsutdinov_lr
--

CREATE TYPE testtype AS (
    field1 text,
    field2 numeric,
    field3 text,
    field4 boolean
);


ALTER TYPE testtype OWNER TO shamsutdinov_lr;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: testtable; Type: TABLE; Schema: public; Owner: shamsutdinov_lr
--

CREATE TABLE testtable OF public.testtype (
    field1 WITH OPTIONS NOT NULL,
    field2 WITH OPTIONS DEFAULT 1000,
    field3 WITH OPTIONS DEFAULT 'word'::text,
    field4 WITH OPTIONS DEFAULT true
);


ALTER TABLE testtable OWNER TO shamsutdinov_lr;

--
-- Name: testtable_pkey; Type: CONSTRAINT; Schema: public; Owner: shamsutdinov_lr
--

ALTER TABLE ONLY testtable
    ADD CONSTRAINT testtable_pkey PRIMARY KEY (field1);


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
