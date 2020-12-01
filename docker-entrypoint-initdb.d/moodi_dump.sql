--
-- PostgreSQL database dump
--

SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;

--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: course; Type: TABLE; Schema: public; Owner: moodi; Tablespace: 
--

CREATE TABLE public.course (
    id bigint NOT NULL,
    realisation_id bigint NOT NULL,
    moodle_id bigint NOT NULL,
    created timestamp without time zone NOT NULL,
    import_status character varying(20) NOT NULL,
    modified timestamp without time zone DEFAULT now() NOT NULL,
    removed boolean DEFAULT false,
    removed_message character varying(30)
);


ALTER TABLE public.course OWNER TO moodi;

--
-- Name: course_enrollment_status; Type: TABLE; Schema: public; Owner: moodi; Tablespace: 
--

CREATE TABLE public.course_enrollment_status (
    id bigint NOT NULL,
    course_realisation_id bigint NOT NULL,
    student_enrollments text,
    teacher_enrollments text,
    created timestamp without time zone NOT NULL,
    course_id bigint NOT NULL
);


ALTER TABLE public.course_enrollment_status OWNER TO moodi;

--
-- Name: course_enrollment_status_id_seq; Type: SEQUENCE; Schema: public; Owner: moodi
--

CREATE SEQUENCE public.course_enrollment_status_id_seq
    START WITH 1
    INCREMENT BY 50
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.course_enrollment_status_id_seq OWNER TO moodi;

--
-- Name: course_id_seq; Type: SEQUENCE; Schema: public; Owner: moodi
--

CREATE SEQUENCE public.course_id_seq
    START WITH 1
    INCREMENT BY 50
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.course_id_seq OWNER TO moodi;

--
-- Name: schema_version; Type: TABLE; Schema: public; Owner: moodi; Tablespace: 
--

CREATE TABLE public.schema_version (
    version_rank integer NOT NULL,
    installed_rank integer NOT NULL,
    version character varying(50) NOT NULL,
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


ALTER TABLE public.schema_version OWNER TO moodi;

--
-- Name: sync_lock; Type: TABLE; Schema: public; Owner: moodi; Tablespace: 
--

CREATE TABLE public.sync_lock (
    id bigint NOT NULL,
    course_id bigint NOT NULL,
    created timestamp without time zone NOT NULL,
    modified timestamp without time zone NOT NULL,
    reason text,
    active boolean DEFAULT true
);


ALTER TABLE public.sync_lock OWNER TO moodi;

--
-- Name: sync_lock_id_seq; Type: SEQUENCE; Schema: public; Owner: moodi
--

CREATE SEQUENCE public.sync_lock_id_seq
    START WITH 1
    INCREMENT BY 50
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.sync_lock_id_seq OWNER TO moodi;

--
-- Name: synchronization_job_run; Type: TABLE; Schema: public; Owner: moodi; Tablespace: 
--

CREATE TABLE public.synchronization_job_run (
    id bigint NOT NULL,
    status character varying(30) NOT NULL,
    type character varying(30) NOT NULL,
    message character varying(2000) NOT NULL,
    started timestamp without time zone NOT NULL,
    completed timestamp without time zone
);


ALTER TABLE public.synchronization_job_run OWNER TO moodi;

--
-- Name: synchronization_job_run_id_seq; Type: SEQUENCE; Schema: public; Owner: moodi
--

CREATE SEQUENCE public.synchronization_job_run_id_seq
    START WITH 1
    INCREMENT BY 50
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.synchronization_job_run_id_seq OWNER TO moodi;

--
-- Data for Name: course; Type: TABLE DATA; Schema: public; Owner: moodi
--

COPY public.course (id, realisation_id, moodle_id, created, import_status, modified, removed, removed_message) FROM stdin;
8252	126466076	1313	2019-02-13 12:31:28.096	COMPLETED	2019-02-27 09:51:12.455	f	\N
7100	109171017	790	2017-04-21 05:28:39.04	COMPLETED	2017-05-05 09:56:36.435	f	\N
7754	121269512	1056	2018-08-28 13:44:34.522	COMPLETED	2018-09-07 13:05:23.132	f	\N
7653	121265655	1013	2018-05-03 06:52:46.274	COMPLETED	2018-09-07 13:05:23.143	f	\N
7605	119969827	1011	2018-04-18 11:18:57.884	COMPLETED	2019-02-27 09:51:12.461	f	\N
7250	109169397	821	2017-06-20 11:49:53.575	COMPLETED	2017-06-20 11:49:55.044	f	\N
7300	109169149	827	2017-08-16 07:15:37.245	COMPLETED	2017-08-16 07:15:42.394	f	\N
7350	109189900	832	2017-09-20 10:44:21.863	COMPLETED	2017-09-20 10:44:22.639	f	\N
7351	109190152	833	2017-09-20 10:59:32.027	COMPLETED	2017-09-20 10:59:32.924	f	\N
7502	121261865	974	2018-03-09 12:09:33.937	COMPLETED	2018-09-07 13:05:23.347	f	\N
7655	114755618	1015	2018-05-03 09:35:04.818	COMPLETED	2018-08-21 12:44:41.974	t	OODI_COURSE_ENDED
7603	120090946	1009	2018-04-13 10:42:04.299	COMPLETED	2019-02-27 09:51:12.467	f	\N
7656	114759921	1016	2018-05-03 10:15:01.459	COMPLETED	2018-08-21 12:44:42.009	t	OODI_COURSE_ENDED
7667	114755778	1027	2018-05-11 06:51:32.122	COMPLETED	2018-08-21 12:44:42.024	t	OODI_COURSE_ENDED
8052	124521263	1138	2018-11-30 08:10:08.2	COMPLETED	2019-02-27 09:51:12.475	f	\N
7955	126430939	1133	2018-10-17 11:43:44.516	COMPLETED	2019-02-27 09:51:12.481	f	\N
8152	124682345	1228	2019-01-11 11:29:57.979	COMPLETED	2019-02-27 09:51:12.49	f	\N
8253	126466290	1314	2019-02-19 13:23:54.732	COMPLETED	2019-02-27 09:51:12.509	f	\N
7902	124278055	1077	2018-10-09 12:19:16.409	COMPLETED	2019-02-27 09:51:12.517	f	\N
7702	121269445	1053	2018-08-21 11:59:54.353	COMPLETED	2018-09-07 13:05:23.397	f	\N
7552	120315594	1007	2018-04-13 10:35:26.991	COMPLETED	2019-02-27 09:51:12.53	f	\N
7660	119851849	1020	2018-05-08 22:52:50.324	COMPLETED	2019-01-10 12:50:19.974	t	OODI_COURSE_ENDED
7503	120458644	975	2018-03-09 12:31:48.95	COMPLETED	2019-01-10 12:50:19.991	t	OODI_COURSE_ENDED
7668	121264509	1028	2018-05-11 10:56:46.922	COMPLETED	2018-09-07 13:05:23.17	f	\N
7669	121254372	1029	2018-05-11 12:48:02.131	COMPLETED	2018-09-07 13:05:23.177	f	\N
7670	121253562	1030	2018-05-11 12:49:28.467	COMPLETED	2018-09-07 13:05:23.186	f	\N
8003	124462876	1136	2018-10-19 08:07:53.384	COMPLETED	2019-02-27 09:51:12.542	f	\N
7752	121269480	1054	2018-08-23 14:20:59.611	COMPLETED	2018-09-07 13:05:23.201	f	\N
7550	120305146	979	2018-03-19 11:17:51.261	COMPLETED	2019-02-27 09:51:12.557	f	\N
7665	120139230	1025	2018-05-11 06:27:11.45	COMPLETED	2018-10-25 10:51:51.881	t	OODI_COURSE_ENDED
104	121264251	1004	2018-04-05 12:04:34.056	COMPLETED	2018-09-07 13:05:23.231	f	\N
7501	120629206	973	2018-03-09 11:16:44.772	COMPLETED	2018-10-25 10:51:51.898	t	OODI_COURSE_REMOVED
7504	121066219	976	2018-03-12 10:20:49.52	COMPLETED	2018-10-25 10:51:51.902	t	OODI_COURSE_ENDED
7604	121228262	1010	2018-04-18 11:17:00.769	COMPLETED	2018-10-25 10:51:51.906	t	OODI_COURSE_ENDED
7663	120741594	1023	2018-05-11 05:48:28.017	COMPLETED	2018-10-25 10:51:51.91	t	OODI_COURSE_ENDED
7666	120409131	1026	2018-05-11 06:49:36.535	COMPLETED	2018-10-25 10:51:51.914	t	OODI_COURSE_ENDED
7602	120902410	1008	2018-04-13 10:40:56.212	COMPLETED	2018-10-25 10:51:51.918	t	OODI_COURSE_ENDED
7662	120741592	1022	2018-05-11 05:38:59.919	COMPLETED	2018-10-25 10:51:51.922	t	OODI_COURSE_ENDED
7661	121266494	1021	2018-05-09 11:32:53.223	COMPLETED	2018-09-07 13:05:23.26	f	\N
7753	121269502	1055	2018-08-24 13:14:50.147	COMPLETED	2018-09-07 13:05:23.281	f	\N
7658	119850874	1018	2018-05-08 22:47:23.616	COMPLETED	2018-10-25 10:51:51.926	t	OODI_COURSE_ENDED
7654	121265653	1014	2018-05-03 07:08:09.945	COMPLETED	2018-09-07 13:05:23.294	f	\N
105	121264342	1005	2018-04-06 09:19:10.478	COMPLETED	2018-09-07 13:05:23.303	f	\N
8004	124462788	1137	2018-10-19 08:14:45.564	COMPLETED	2019-02-27 09:51:12.567	f	\N
7551	120593802	980	2018-03-19 11:34:39.483	COMPLETED	2019-02-27 09:51:12.576	f	\N
7953	126431132	1131	2018-10-17 11:13:00.769	COMPLETED	2019-02-27 09:51:12.583	f	\N
7400	121252402	838	2017-11-30 13:57:30.591	COMPLETED	2018-09-07 13:05:23.33	f	\N
7952	126433427	1130	2018-10-17 11:03:03.751	COMPLETED	2019-02-27 09:51:12.594	f	\N
103	116070758	992	2018-04-05 09:37:32.887	COMPLETED	2018-08-16 11:40:37.194	t	OODI_COURSE_ENDED
7659	120167127	1019	2018-05-08 22:48:42.943	COMPLETED	2018-10-30 11:06:13.458	t	OODI_COURSE_ENDED
106	120314692	1006	2018-04-11 11:16:11.856	COMPLETED	2019-02-13 12:36:14.042	t	OODI_COURSE_ENDED
7505	120982192	977	2018-03-12 11:24:58.898	COMPLETED	2019-02-13 12:36:14.056	t	OODI_COURSE_ENDED
8002	126459109	1135	2018-10-18 12:51:46.012	COMPLETED	2019-02-27 09:51:12.601	f	\N
8202	126459114	1230	2019-01-29 16:43:20.156	COMPLETED	2019-02-27 09:51:12.617	f	\N
7500	121252746	971	2018-03-08 11:20:36.04	COMPLETED	2018-09-07 13:05:23.342	f	\N
7956	124692512	1134	2018-10-17 11:44:57.993	COMPLETED	2019-02-27 09:51:12.634	f	\N
7802	121269948	1057	2018-09-07 12:55:04.573	COMPLETED	2018-09-07 13:05:23.079	f	\N
7664	120462560	1024	2018-05-11 05:54:53.995	COMPLETED	2019-02-27 09:51:12.177	f	\N
7652	121264920	1012	2018-04-20 12:45:30.634	COMPLETED	2018-09-07 13:05:23.095	f	\N
8102	126463875	1227	2019-01-10 12:31:53.474	COMPLETED	2019-02-27 09:51:12.19	f	\N
7954	126431139	1132	2018-10-17 11:41:36.088	COMPLETED	2019-02-27 09:51:12.197	f	\N
7450	120294365	970	2018-02-07 07:02:46.907	COMPLETED	2019-02-27 09:51:12.205	f	\N
8302	126466643	1315	2019-02-27 09:44:10.517	COMPLETED	2019-02-27 09:51:12.437	f	\N
7657	119850673	1017	2018-05-08 22:46:30.175	COMPLETED	2018-11-30 10:33:48.254	t	OODI_COURSE_ENDED
7506	120591826	978	2018-03-12 11:52:32.362	COMPLETED	2018-11-30 10:33:48.266	t	OODI_COURSE_ENDED
7852	123764988	1069	2018-10-03 08:09:42.189	COMPLETED	2019-02-27 09:51:12.446	f	\N
8352	126494603	1395	2019-11-13 12:35:33.13	COMPLETED	2019-11-13 12:35:36.598	f	\N
8153	126464298	1229	2019-01-11 12:18:20.047	COMPLETED	2019-02-27 09:51:12.498	f	\N
8353	126494604	1396	2019-11-13 13:49:07.023	COMPLETED	2019-11-13 13:49:08.832	f	\N
8354	126494607	1397	2019-11-13 14:00:44.328	COMPLETED	2019-11-13 14:00:44.74	f	\N
8402	130528153	1458	2020-04-27 06:55:12.24	COMPLETED	2020-04-27 06:55:29.422	f	\N
8452	130779665	1470	2020-05-13 09:41:46.896	COMPLETED	2020-05-13 09:41:47.024	f	\N
8502	131991916	1471	2020-05-29 10:49:25.461	COMPLETED	2020-05-29 10:49:25.757	f	\N
8503	128664025	1472	2020-05-29 11:13:17.394	COMPLETED	2020-05-29 11:13:17.415	f	\N
8552	129859594	1473	2020-06-02 10:11:25.04	COMPLETED	2020-06-02 10:11:25.274	f	\N
8553	129726668	1474	2020-06-03 10:11:03.611	COMPLETED	2020-06-03 10:11:03.642	f	\N
8554	132028584	1475	2020-06-03 10:26:27.108	COMPLETED	2020-06-03 10:26:27.126	f	\N
8555	129616688	1476	2020-06-03 10:34:56.085	COMPLETED	2020-06-03 10:34:56.1	f	\N
8556	130682532	1477	2020-06-04 08:07:47.737	COMPLETED	2020-06-04 08:07:47.768	f	\N
8557	130852727	1478	2020-06-08 11:19:15.832	COMPLETED	2020-06-08 11:19:15.868	f	\N
8558	129814895	1479	2020-06-08 12:49:49.724	COMPLETED	2020-06-08 12:49:49.755	f	\N
8559	132410354	1480	2020-06-09 11:48:30.593	COMPLETED	2020-06-09 11:48:32.896	f	\N
8560	129938357	1481	2020-06-10 13:25:22.386	COMPLETED	2020-06-10 13:25:24.898	f	\N
8561	130859367	1482	2020-06-15 12:23:47.081	COMPLETED	2020-06-15 12:23:48.459	f	\N
8562	132209133	1483	2020-06-15 12:24:45.047	COMPLETED	2020-06-15 12:24:45.056	f	\N
8563	132256556	1484	2020-06-15 12:58:27.983	COMPLETED	2020-06-15 12:58:27.992	f	\N
8564	132409026	1485	2020-06-16 08:23:54.155	COMPLETED	2020-06-16 08:23:54.936	f	\N
8602	132398493	1497	2020-06-24 14:08:48.626	COMPLETED	2020-06-24 14:08:48.886	f	\N
8603	132398495	1498	2020-06-24 14:50:48.579	COMPLETED	2020-06-24 14:50:48.592	f	\N
8604	132398491	1499	2020-06-24 15:31:52.332	COMPLETED	2020-06-24 15:31:52.357	f	\N
8605	129785834	1500	2020-06-25 07:27:43.573	COMPLETED	2020-06-25 07:27:43.639	f	\N
8606	129785838	1501	2020-06-25 07:37:41.461	COMPLETED	2020-06-25 07:37:41.479	f	\N
8607	129785786	1502	2020-06-25 09:08:36.599	COMPLETED	2020-06-25 09:08:36.625	f	\N
8608	129785780	1503	2020-06-25 09:15:12.613	COMPLETED	2020-06-25 09:15:12.626	f	\N
8609	132415044	1504	2020-07-02 10:12:40.744	COMPLETED	2020-07-02 10:12:40.787	f	\N
8610	132402182	1505	2020-07-02 10:36:53.141	COMPLETED	2020-07-02 10:36:53.151	f	\N
8611	132412424	1506	2020-07-02 10:53:47.946	COMPLETED	2020-07-02 10:53:47.978	f	\N
8612	132413790	1507	2020-07-02 11:52:03.483	COMPLETED	2020-07-02 11:52:03.503	f	\N
8613	132412853	1508	2020-07-02 13:18:50.595	COMPLETED	2020-07-02 13:18:50.614	f	\N
8614	132398506	1509	2020-07-02 13:25:53.537	COMPLETED	2020-07-02 13:25:53.551	f	\N
8615	132414615	1510	2020-07-02 13:32:26.19	COMPLETED	2020-07-02 13:32:26.203	f	\N
8616	132401998	1511	2020-07-03 06:51:42.732	COMPLETED	2020-07-03 06:51:42.753	f	\N
8652	130807911	1512	2020-08-06 12:48:43.412	COMPLETED	2020-08-06 12:48:43.761	f	\N
8653	129542550	1513	2020-08-07 12:33:30.258	COMPLETED	2020-08-07 12:33:30.294	f	\N
8702	132402177	1514	2020-09-03 07:54:59.636	COMPLETED	2020-09-03 07:54:59.912	f	\N
8703	132409796	1515	2020-09-03 10:20:00.8	COMPLETED	2020-09-03 10:20:00.842	f	\N
8752	132507174	1527	2020-09-22 11:39:45.815	COMPLETED	2020-09-22 11:39:58.857	f	\N
8753	132504453	1528	2020-09-23 06:43:14.335	COMPLETED	2020-09-23 06:43:14.369	f	\N
8802	132507166	72	2020-10-12 06:47:41.313	COMPLETED	2020-10-12 06:47:51.27	f	\N
8803	132507373	73	2020-10-12 07:33:42.654	COMPLETED	2020-10-12 07:33:44.64	f	\N
8804	132409934	74	2020-10-12 11:17:22.841	COMPLETED	2020-10-12 11:17:22.863	f	\N
8852	132398632	75	2020-10-22 07:33:25.087	COMPLETED	2020-10-22 07:33:25.317	f	\N
8902	130824531	76	2020-11-04 13:00:11.822	COMPLETED	2020-11-04 13:00:11.888	f	\N
8903	136813170	77	2020-11-04 13:06:40.828	COMPLETED	2020-11-04 13:06:40.84	f	\N
8904	136572307	100	2020-11-17 12:05:33.835	COMPLETED	2020-11-17 12:05:39.121	f	\N
8952	136688819	101	2020-11-19 10:41:01.726	COMPLETED	2020-11-19 10:41:03.3	f	\N
\.


--
-- Data for Name: course_enrollment_status; Type: TABLE DATA; Schema: public; Owner: moodi
--

COPY public.course_enrollment_status (id, course_realisation_id, student_enrollments, teacher_enrollments, created, course_id) FROM stdin;
\.


--
-- Name: course_enrollment_status_id_seq; Type: SEQUENCE SET; Schema: public; Owner: moodi
--

SELECT pg_catalog.setval('public.course_enrollment_status_id_seq', 0, true);


--
-- Name: course_id_seq; Type: SEQUENCE SET; Schema: public; Owner: moodi
--

SELECT pg_catalog.setval('public.course_id_seq', 9001, true);


--
-- Data for Name: schema_version; Type: TABLE DATA; Schema: public; Owner: moodi
--

COPY public.schema_version (version_rank, installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	1	001.00	init	SQL	V001.00__init.sql	-599570833	moodi	2015-09-14 13:51:54.87978	16	t
2	2	002.00	sync jobs	SQL	V002.00__sync_jobs.sql	-1487786753	moodi	2015-09-14 13:51:54.951141	16	t
3	3	003.00	alter sequences	SQL	V003.00__alter_sequences.sql	-1804484160	moodi	2015-09-14 13:51:54.997056	5	t
4	4	004.00	course enrollment status	SQL	V004.00__course_enrollment_status.sql	-634934123	moodi	2015-11-19 14:31:36.930671	829	t
5	5	005.00	indexes and constraints	SQL	V005.00__indexes_and_constraints.sql	468134597	moodi	2016-04-27 14:25:45.447399	191	t
6	6	006.00	course import status	SQL	V006.00__course_import_status.sql	1288653598	moodi	2016-06-17 14:24:52.039752	1535	t
7	7	007.00	new course fields	SQL	V007.00__new_course_fields.sql	-1445391379	moodi	2016-07-07 12:42:41.220347	440	t
8	8	008.00	sync lock	SQL	V008.00__sync_lock.sql	871886925	moodi	2016-11-17 20:13:36.581718	1078	t
9	9	009.00	restart sequences	SQL	V009.00__restart_sequences.sql	-137647301	moodi	2018-04-13 11:21:33.115169	1046	t
\.


--
-- Data for Name: sync_lock; Type: TABLE DATA; Schema: public; Owner: moodi
--

COPY public.sync_lock (id, course_id, created, modified, reason, active) FROM stdin;
\.


--
-- Name: sync_lock_id_seq; Type: SEQUENCE SET; Schema: public; Owner: moodi
--

SELECT pg_catalog.setval('public.sync_lock_id_seq', 0, true);


--
-- Data for Name: synchronization_job_run; Type: TABLE DATA; Schema: public; Owner: moodi
--

COPY public.synchronization_job_run (id, status, type, message, started, completed) FROM stdin;
\.


--
-- Name: synchronization_job_run_id_seq; Type: SEQUENCE SET; Schema: public; Owner: moodi
--

SELECT pg_catalog.setval('public.synchronization_job_run_id_seq', 900, true);


--
-- Name: course_enrollment_status_pkey; Type: CONSTRAINT; Schema: public; Owner: moodi; Tablespace: 
--

ALTER TABLE ONLY public.course_enrollment_status
    ADD CONSTRAINT course_enrollment_status_pkey PRIMARY KEY (id);


--
-- Name: course_pkey; Type: CONSTRAINT; Schema: public; Owner: moodi; Tablespace: 
--

ALTER TABLE ONLY public.course
    ADD CONSTRAINT course_pkey PRIMARY KEY (id);


--
-- Name: schema_version_pk; Type: CONSTRAINT; Schema: public; Owner: moodi; Tablespace: 
--

ALTER TABLE ONLY public.schema_version
    ADD CONSTRAINT schema_version_pk PRIMARY KEY (version);


--
-- Name: sync_lock_pkey; Type: CONSTRAINT; Schema: public; Owner: moodi; Tablespace: 
--

ALTER TABLE ONLY public.sync_lock
    ADD CONSTRAINT sync_lock_pkey PRIMARY KEY (id);


--
-- Name: synchronization_job_run_pkey; Type: CONSTRAINT; Schema: public; Owner: moodi; Tablespace: 
--

ALTER TABLE ONLY public.synchronization_job_run
    ADD CONSTRAINT synchronization_job_run_pkey PRIMARY KEY (id);


--
-- Name: course_moodle_id_index; Type: INDEX; Schema: public; Owner: moodi; Tablespace: 
--

CREATE UNIQUE INDEX course_moodle_id_index ON public.course USING btree (moodle_id);


--
-- Name: course_realisation_id_index; Type: INDEX; Schema: public; Owner: moodi; Tablespace: 
--

CREATE UNIQUE INDEX course_realisation_id_index ON public.course USING btree (realisation_id);


--
-- Name: schema_version_ir_idx; Type: INDEX; Schema: public; Owner: moodi; Tablespace: 
--

CREATE INDEX schema_version_ir_idx ON public.schema_version USING btree (installed_rank);


--
-- Name: schema_version_s_idx; Type: INDEX; Schema: public; Owner: moodi; Tablespace: 
--

CREATE INDEX schema_version_s_idx ON public.schema_version USING btree (success);


--
-- Name: schema_version_vr_idx; Type: INDEX; Schema: public; Owner: moodi; Tablespace: 
--

CREATE INDEX schema_version_vr_idx ON public.schema_version USING btree (version_rank);


--
-- Name: course_enrollment_status_course_id; Type: FK CONSTRAINT; Schema: public; Owner: moodi
--

ALTER TABLE ONLY public.course_enrollment_status
    ADD CONSTRAINT course_enrollment_status_course_id FOREIGN KEY (course_id) REFERENCES public.course(id) ON DELETE CASCADE;


--
-- Name: sync_lock_course_fk; Type: FK CONSTRAINT; Schema: public; Owner: moodi
--

ALTER TABLE ONLY public.sync_lock
    ADD CONSTRAINT sync_lock_course_fk FOREIGN KEY (course_id) REFERENCES public.course(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

