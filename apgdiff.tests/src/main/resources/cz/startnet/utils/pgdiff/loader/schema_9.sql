CREATE TABLE user_data (
    id bigint NOT NULL,
    email character varying(128) NOT NULL,
    created timestamp with time zone DEFAULT now()
);

ALTER TABLE public.user_data OWNER TO postgres;

CREATE SEQUENCE user_id_seq
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

ALTER TABLE public.user_id_seq OWNER TO postgres;

ALTER SEQUENCE user_id_seq OWNED BY user_data.id;

ALTER TABLE user_data ALTER COLUMN id SET DEFAULT nextval('user_id_seq'::regclass);

CREATE VIEW "user" AS
    (       SELECT 
                user_data.id, 
                user_data.email, 
                user_data.created 
            FROM 
                user_data);

ALTER TABLE public."user" OWNER TO postgres;
ALTER VIEW public."user" OWNER TO postgres;

--ALTER TABLE "user" ALTER COLUMN created SET DEFAULT now();
ALTER VIEW "user" ALTER COLUMN created SET DEFAULT now();


CREATE VIEW ws_test AS 

SELECT
ud.id
"   i   d   "
FROM user_data ud;

CREATE TABLE t_ruleinsert(c1 int);

CREATE RULE on_delete AS ON DELETE TO "user" DO ALSO DELETE FROM user_data WHERE (user_data.id = old.id);
CREATE RULE on_insert AS ON INSERT TO "user" DO INSTEAD (INSERT INTO user_data (id, email, created) VALUES (new.id, new.email, new.created); INSERT INTO t1(c1) DEFAULT VALUES);
CREATE RULE on_update AS ON UPDATE TO "user" DO INSTEAD (UPDATE user_data SET id = new.id, email = new.email, created = new.created WHERE (user_data.id = old.id));
CREATE RULE on_select AS ON SELECT TO user_data WHERE (1=1) DO INSTEAD NOTHING;