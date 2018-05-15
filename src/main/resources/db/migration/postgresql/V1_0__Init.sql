
CREATE TABLE luzern.table_shcema
(
  id bigserial NOT NULL,
  description character varying(255),
  name character varying(20) NOT NULL,
  predefine boolean,
  CONSTRAINT table_shcema_pkey PRIMARY KEY (id),
  CONSTRAINT uk_f05w8no8sdp7wp6igbgcanebm UNIQUE (name)
);

INSERT INTO luzern.table_shcema(id, description, name, predefine) VALUES ('1', 'Default Table', 'default', true);
SELECT setval('luzern.table_shcema_id_seq', 10, true);

CREATE TABLE luzern.column_info
(
  id bigserial NOT NULL,
  alias character varying(50),
  description character varying(255),
  log_field_type character varying(255) NOT NULL,
  name character varying(50) NOT NULL,
  predefine boolean,
  search boolean,
  statistics boolean,
  table_id bigint NOT NULL,
  CONSTRAINT column_info_pkey PRIMARY KEY (id),
  CONSTRAINT fkrgoxsvum6jop3kdyvqedtp153 FOREIGN KEY (table_id)
      REFERENCES luzern.table_shcema (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE,
  CONSTRAINT uk91uvcqx00lu732dcdolvja1yh UNIQUE (table_id, name)
);

CREATE TABLE luzern.generator_info
(
  id bigserial NOT NULL,
  content character varying(65535) NOT NULL,
  name character varying(50) NOT NULL,
  CONSTRAINT generator_info_pkey PRIMARY KEY (id)
);

CREATE TABLE luzern.mapping_info
(
  id bigserial NOT NULL,
  content character varying(65535) NOT NULL,
  description character varying(255),
  modify_date timestamp without time zone NOT NULL,
  name character varying(50) NOT NULL,
  predefine boolean,
  CONSTRAINT mapping_info_pkey PRIMARY KEY (id)
);

CREATE TABLE luzern.script_info
(
  id bigserial NOT NULL,
  description character varying(255),
  modify_date timestamp without time zone NOT NULL,
  name character varying(50) NOT NULL,
  predefine boolean,
  script character varying(65535) NOT NULL,
  table_id bigint NOT NULL,
  CONSTRAINT script_info_pkey PRIMARY KEY (id),
  CONSTRAINT fks6gmjivulvua2q4wqvc9vkk9n FOREIGN KEY (table_id)
      REFERENCES luzern.table_shcema (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE,
  CONSTRAINT ukqcw36fboqqdg7t2pbe2bcb4yo UNIQUE (table_id, name)
);

CREATE TABLE luzern.parser_info
(
  id bigserial NOT NULL,
  description character varying(255) NOT NULL,
  encoding character varying(50) NOT NULL,
  ip character varying(50) NOT NULL,
  predefine boolean,
  script_info_id bigint NOT NULL,
  CONSTRAINT parser_info_pkey PRIMARY KEY (id),
  CONSTRAINT fktily0pvyvmp4hjctseocua3ee FOREIGN KEY (script_info_id)
      REFERENCES luzern.script_info (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE,
  CONSTRAINT uk_l0y073irtr3yvvuruw0o1avjv UNIQUE (ip)
);

CREATE TABLE luzern.filemonitoring_info
(
  id bigserial NOT NULL,
  path character varying(255) NOT NULL,
  description character varying(255),
  modify_date timestamp without time zone NOT NULL,
  enable boolean,
  script_info_id bigint NOT NULL,
  CONSTRAINT filemonitoring_info_pkey PRIMARY KEY (id),
  CONSTRAINT filemonitoring_info_fkey1 FOREIGN KEY (script_info_id)
      REFERENCES luzern.script_info (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE,
  CONSTRAINT filemonitoring_info_unique UNIQUE (path)
);
