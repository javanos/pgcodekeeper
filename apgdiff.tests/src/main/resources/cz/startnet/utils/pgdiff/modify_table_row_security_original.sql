--
-- PostgreSQL database dump
--

SET client_encoding = 'UTF8';
SET check_function_bodies = false;
SET client_min_messages = warning;

--
-- Name: SCHEMA public; Type: COMMENT; Schema: -; Owner: postgres
--

COMMENT ON SCHEMA public IS 'Standard public schema';


SET search_path = public, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: testtable; Type: TABLE; Schema: public; Owner: fordfrog; Tablespace: 
--

CREATE TABLE testtable (
    field1 integer,
    field2 integer,
    field3 character varying(150) DEFAULT 'none'::character varying,
    field4 double precision
);

ALTER TABLE ONLY testtable FORCE ROW LEVEL SECURITY;

ALTER TABLE public.testtable OWNER TO fordfrog;

--
-- Name: testtable2; Type: TABLE; Schema: public; Owner: fordfrog; Tablespace: 
--

CREATE TABLE testtable2 (
    field1 integer,
    field2 integer
);

ALTER TABLE public.testtable2 OWNER TO fordfrog;

--
-- Name: testtable3; Type: TABLE; Schema: public; Owner: fordfrog; Tablespace: 
--

CREATE TABLE testtable3 (
    id integer,
    name character varying(100) NOT NULL
);

ALTER TABLE ONLY testtable3 FORCE ROW LEVEL SECURITY;

ALTER TABLE public.testtable3 OWNER TO fordfrog;

--
-- Name: testindex; Type: INDEX; Schema: public; Owner: fordfrog; Tablespace: 
--

CREATE INDEX testindex ON testtable USING btree (field3);

--
-- Name: testtable2; Type: ROW SECURITY; Schema: public; Owner: galiev_mr
--

ALTER TABLE testtable2 ENABLE ROW LEVEL SECURITY;

--
-- Name: testtable3; Type: ROW SECURITY; Schema: public; Owner: galiev_mr
--

ALTER TABLE testtable3 ENABLE ROW LEVEL SECURITY;

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

