CREATE TABLE table_shcema
(
  id bigserial NOT NULL,
  description character varying(255),
  name character varying(20) NOT NULL,
  predefine boolean,
  CONSTRAINT table_shcema_pkey PRIMARY KEY (id),
  CONSTRAINT uk_f05w8no8sdp7wp6igbgcanebm UNIQUE (name)
);

CREATE TABLE column_info
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
      REFERENCES table_shcema ON DELETE CASCADE,
  CONSTRAINT uk91uvcqx00lu732dcdolvja1yh UNIQUE (table_id, name)
);

CREATE TABLE generator_info
(
  id bigserial NOT NULL,
  content character varying(65535) NOT NULL,
  name character varying(50) NOT NULL,
  CONSTRAINT generator_info_pkey PRIMARY KEY (id)
);

CREATE TABLE mapping_info
(
  id bigserial NOT NULL,
  content character varying(65535) NOT NULL,
  description character varying(255),
  modify_date timestamp not null,
  name character varying(50) NOT NULL,
  predefine boolean,
  CONSTRAINT mapping_info_pkey PRIMARY KEY (id)
);

CREATE TABLE script_info
(
  id bigserial NOT NULL,
  description character varying(255),
  modify_date timestamp not null,
  name character varying(50) NOT NULL,
  predefine boolean,
  script character varying(65535) NOT NULL,
  table_id bigint NOT NULL,
  CONSTRAINT script_info_pkey PRIMARY KEY (id),
  CONSTRAINT fks6gmjivulvua2q4wqvc9vkk9n FOREIGN KEY (table_id)
      REFERENCES table_shcema ON DELETE CASCADE,
  CONSTRAINT ukqcw36fboqqdg7t2pbe2bcb4yo UNIQUE (table_id, name)
);

CREATE TABLE parser_info
(
  id bigserial NOT NULL,
  description character varying(255) NOT NULL,
  encoding character varying(50) NOT NULL,
  ip character varying(50) NOT NULL,
  predefine boolean,
  script_info_id bigint NOT NULL,
  CONSTRAINT parser_info_pkey PRIMARY KEY (id),
  CONSTRAINT fktily0pvyvmp4hjctseocua3ee FOREIGN KEY (script_info_id)
      REFERENCES script_info ON DELETE CASCADE,
  CONSTRAINT uk_l0y073irtr3yvvuruw0o1avjv UNIQUE (ip)
);

INSERT INTO table_shcema(description, name, predefine) VALUES ('Default Table', 'default', true);