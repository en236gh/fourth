--
-- PostgreSQL database dump
--

\restrict 8dwg92qoVXDfZeS5hpwvnPTrVGua4M0zfEg1sL2f1CpPbeKwOPcNSyBmTzsbogu

-- Dumped from database version 18.0
-- Dumped by pg_dump version 18.0

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: pgcrypto; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;


--
-- Name: EXTENSION pgcrypto; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION pgcrypto IS 'cryptographic functions';


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: attendance_record; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.attendance_record (
    attendance_id integer NOT NULL,
    computer_number character varying(15) NOT NULL,
    exam_session_id integer NOT NULL,
    check_in_time timestamp without time zone NOT NULL,
    verification_method character varying(20) NOT NULL,
    verified_by_staff_id integer NOT NULL,
    check_in_venue_id integer NOT NULL,
    status character varying(20) NOT NULL,
    scripts_submitted boolean DEFAULT false NOT NULL,
    alert_message text,
    CONSTRAINT attendance_record_status_check CHECK (((status)::text = ANY ((ARRAY['PRESENT'::character varying, 'ABSENT'::character varying, 'LATE'::character varying, 'WRONG_VENUE'::character varying])::text[]))),
    CONSTRAINT attendance_record_verification_method_check CHECK (((verification_method)::text = ANY ((ARRAY['QR_CODE'::character varying, 'FACIAL_RECOGNITION'::character varying, 'QR_AND_FACIAL'::character varying])::text[])))
);


ALTER TABLE public.attendance_record OWNER TO postgres;

--
-- Name: attendance_record_attendance_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.attendance_record ALTER COLUMN attendance_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.attendance_record_attendance_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: course; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.course (
    course_code character varying(15) NOT NULL,
    course_name character varying(150) NOT NULL,
    credit_hours smallint NOT NULL,
    department character varying(120) NOT NULL
);


ALTER TABLE public.course OWNER TO postgres;

--
-- Name: course_lecturer; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.course_lecturer (
    course_code character varying(15) NOT NULL,
    staff_id integer NOT NULL
);


ALTER TABLE public.course_lecturer OWNER TO postgres;

--
-- Name: exam_session; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.exam_session (
    exam_session_id integer NOT NULL,
    course_code character varying(15) NOT NULL,
    exam_date date NOT NULL,
    start_time time without time zone NOT NULL,
    end_time time without time zone NOT NULL,
    academic_year character varying(9) NOT NULL,
    semester smallint NOT NULL,
    exam_type character varying(20) DEFAULT 'FINAL'::character varying NOT NULL,
    CONSTRAINT exam_session_check CHECK ((end_time > start_time)),
    CONSTRAINT exam_session_exam_type_check CHECK (((exam_type)::text = ANY ((ARRAY['FINAL'::character varying, 'SUPPLEMENTARY'::character varying, 'SPECIAL'::character varying])::text[]))),
    CONSTRAINT exam_session_semester_check CHECK ((semester = ANY (ARRAY[1, 2])))
);


ALTER TABLE public.exam_session OWNER TO postgres;

--
-- Name: exam_session_exam_session_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.exam_session ALTER COLUMN exam_session_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.exam_session_exam_session_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: exam_venue; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.exam_venue (
    exam_session_id integer NOT NULL,
    venue_id integer NOT NULL
);


ALTER TABLE public.exam_venue OWNER TO postgres;

--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


ALTER TABLE public.flyway_schema_history OWNER TO postgres;

--
-- Name: incidence; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.incidence (
    incidence_id bigint NOT NULL,
    exam_session_id bigint NOT NULL,
    venue_id bigint,
    reported_by_staff_id bigint NOT NULL,
    student_computer_number character varying(50),
    type character varying(30) NOT NULL,
    description character varying(1000) NOT NULL,
    reported_at timestamp without time zone NOT NULL
);


ALTER TABLE public.incidence OWNER TO postgres;

--
-- Name: incidence_incidence_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.incidence_incidence_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.incidence_incidence_id_seq OWNER TO postgres;

--
-- Name: incidence_incidence_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.incidence_incidence_id_seq OWNED BY public.incidence.incidence_id;


--
-- Name: incident_report; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.incident_report (
    incident_id integer NOT NULL,
    exam_session_id integer NOT NULL,
    computer_number character varying(15),
    reported_by_staff_id integer NOT NULL,
    incident_time timestamp without time zone NOT NULL,
    description character varying(255) NOT NULL,
    severity character varying(20) DEFAULT 'MINOR'::character varying NOT NULL,
    CONSTRAINT incident_report_severity_check CHECK (((severity)::text = ANY ((ARRAY['MINOR'::character varying, 'MAJOR'::character varying, 'CRITICAL'::character varying])::text[])))
);


ALTER TABLE public.incident_report OWNER TO postgres;

--
-- Name: incident_report_incident_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.incident_report ALTER COLUMN incident_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.incident_report_incident_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: invigilator_assignment; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.invigilator_assignment (
    exam_session_id integer NOT NULL,
    venue_id integer NOT NULL,
    staff_id integer NOT NULL
);


ALTER TABLE public.invigilator_assignment OWNER TO postgres;

--
-- Name: staff; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.staff (
    staff_id integer NOT NULL,
    staff_no character varying(15) NOT NULL,
    full_name character varying(120) NOT NULL,
    email character varying(120) NOT NULL,
    phone character varying(20),
    department character varying(120),
    password_hash character varying(255) NOT NULL
);


ALTER TABLE public.staff OWNER TO postgres;

--
-- Name: staff_role; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.staff_role (
    staff_id integer NOT NULL,
    role character varying(20) NOT NULL,
    CONSTRAINT staff_role_role_check CHECK (((role)::text = ANY ((ARRAY['INVIGILATOR'::character varying, 'ADMINISTRATOR'::character varying, 'LECTURER'::character varying])::text[])))
);


ALTER TABLE public.staff_role OWNER TO postgres;

--
-- Name: staff_staff_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.staff ALTER COLUMN staff_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.staff_staff_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: student; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.student (
    computer_number character varying(15) NOT NULL,
    national_id character varying(20) NOT NULL,
    full_name character varying(120) NOT NULL,
    program character varying(120) NOT NULL,
    year_of_study smallint NOT NULL,
    email character varying(120),
    phone character varying(20),
    photo_path character varying(255) NOT NULL,
    qr_token character varying(255) NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    CONSTRAINT student_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'SUSPENDED'::character varying, 'GRADUATED'::character varying, 'DEFERRED'::character varying])::text[]))),
    CONSTRAINT student_year_of_study_check CHECK (((year_of_study >= 1) AND (year_of_study <= 7)))
);


ALTER TABLE public.student OWNER TO postgres;

--
-- Name: student_registration; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.student_registration (
    computer_number character varying(15) NOT NULL,
    course_code character varying(15) NOT NULL,
    academic_year character varying(9) NOT NULL,
    semester smallint NOT NULL,
    CONSTRAINT student_registration_semester_check CHECK ((semester = ANY (ARRAY[1, 2])))
);


ALTER TABLE public.student_registration OWNER TO postgres;

--
-- Name: student_venue_allocation; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.student_venue_allocation (
    computer_number character varying(15) NOT NULL,
    exam_session_id integer NOT NULL,
    venue_id integer NOT NULL,
    seat_number character varying(10)
);


ALTER TABLE public.student_venue_allocation OWNER TO postgres;

--
-- Name: venue; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.venue (
    venue_id integer NOT NULL,
    venue_name character varying(100) NOT NULL,
    building character varying(100) NOT NULL,
    capacity integer NOT NULL,
    CONSTRAINT venue_capacity_check CHECK ((capacity > 0))
);


ALTER TABLE public.venue OWNER TO postgres;

--
-- Name: venue_venue_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.venue ALTER COLUMN venue_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.venue_venue_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: incidence incidence_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incidence ALTER COLUMN incidence_id SET DEFAULT nextval('public.incidence_incidence_id_seq'::regclass);


--
-- Data for Name: attendance_record; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.attendance_record (attendance_id, computer_number, exam_session_id, check_in_time, verification_method, verified_by_staff_id, check_in_venue_id, status, scripts_submitted, alert_message) FROM stdin;
1	CS-2023-001	4	2026-07-13 20:54:47.870034	QR_CODE	14	1	LATE	f	\N
\.


--
-- Data for Name: course; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.course (course_code, course_name, credit_hours, department) FROM stdin;
CSC101	Introduction to Programming	4	Computer Science
\.


--
-- Data for Name: course_lecturer; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.course_lecturer (course_code, staff_id) FROM stdin;
CSC101	3
\.


--
-- Data for Name: exam_session; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.exam_session (exam_session_id, course_code, exam_date, start_time, end_time, academic_year, semester, exam_type) FROM stdin;
1	CSC101	2026-07-12	09:00:00	11:00:00	2025-2026	1	FINAL
2	CSC101	2026-07-12	09:00:00	11:00:00	2025-2026	1	FINAL
3	CSC101	2026-07-12	09:00:00	11:00:00	2025-2026	1	FINAL
4	CSC101	2026-07-13	09:00:00	11:00:00	2025/2026	1	FINAL
\.


--
-- Data for Name: exam_venue; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.exam_venue (exam_session_id, venue_id) FROM stdin;
4	1
\.


--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	1	<< Flyway Baseline >>	BASELINE	<< Flyway Baseline >>	\N	postgres	2026-07-13 21:23:21.45335	0	t
2	2	add alert message to attendance record	SQL	V2__add_alert_message_to_attendance_record.sql	2003978427	postgres	2026-07-16 17:56:23.404751	61	t
3	3	create incidence table	SQL	V3__create_incidence_table.sql	698868138	postgres	2026-07-16 18:18:34.562444	1376	t
\.


--
-- Data for Name: incidence; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.incidence (incidence_id, exam_session_id, venue_id, reported_by_staff_id, student_computer_number, type, description, reported_at) FROM stdin;
\.


--
-- Data for Name: incident_report; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.incident_report (incident_id, exam_session_id, computer_number, reported_by_staff_id, incident_time, description, severity) FROM stdin;
1	1	CS-2023-001	1	2026-07-12 16:37:12.50334	Student arrived 15 minutes late to the exam without valid reason.	MINOR
2	1	CS-2023-002	1	2026-07-12 16:37:13.048234	Student was found with unauthorized materials during the examination.	MAJOR
3	1	CS-2023-001	1	2026-07-12 16:37:13.365154	Student was impersonating another candidate. Security has been notified.	CRITICAL
\.


--
-- Data for Name: invigilator_assignment; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.invigilator_assignment (exam_session_id, venue_id, staff_id) FROM stdin;
\.


--
-- Data for Name: staff; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.staff (staff_id, staff_no, full_name, email, phone, department, password_hash) FROM stdin;
3	LEC001	Dr. Lecturer	lecturer@unza.zm	+260977000003	Computer Science	$2a$10$BVWMZyQkHZF9F2suC1V3fOHdd/TMwovg6wMo8v0Abj4eQpxhSLFDS
10	INV002	Jane Invigilator	jane.invigilator@unza.zm	+260977000021	Physics	$2a$10$q/FlXugZRiMm0lH.MPz8wOXCyxOQa4GMRZ4QqO0YzXj.qRYCyQkui
11	LEC002	Prof. Lecturer	prof.lecturer@unza.zm	+260977000022	Mathematics	$2a$10$vqfg/EiT.Fb3mxsrZ494.upW2caR0Mh4KafFs3HFuT4JaWvp8tcwe
12	ADMIN002	Secondary Admin	admin2@unza.zm	+260977000020	Registry	$2a$10$OJbQYuQClB5C6M2B7RW61.9gUVaNUF4sbt3KuC.rnkdqoFzhZuczG
2	INV001	John Invigilator	invigilator@unza.zm	+260977000002	Mathematics	$2a$10$bTNwOTWFmg4FVjozP5Bs5.7Ry9l.lpTFz3ZUk5rF8RfeuL88S5FpS
1	ADMIN001	System Administrator	admin@unza.zm	+260977000001	Examinations Office	$2a$10$Ul83hqhD2vhaT9sR23aftOZyFrsA3viy4Epx8YFnn.jsPkf7RdGTK
13	INV2393.73087	Postman Test Invigilator	invigilator.2393.73087@unza.zm	\N	\N	$2a$10$t8xK0YgE1lrjoyAR6Kx/Me.HgY1htWurmE.bDREKJa6Z/hcMwKpBS
14	INV3976082782	Postman Test Invigilator	invigilator.3976082782@unza.zm	\N	\N	$2a$10$j0ONyx36zyk1CYf0wkHHaOa/9oY.VoMLgB1GScv5lGHObF.6rD4vC
15	INV3977358576	Postman Test Invigilator	invigilator.3977358576@unza.zm	\N	\N	$2a$10$o/YQhZqW9S5SlPB5NFK4quPAFx4TjA35d8zTNcfThRr1MztPIjjR2
16	INV3977840731	Postman Test Invigilator	invigilator.3977840731@unza.zm	\N	\N	$2a$10$cuacvDpGysy0xpCimWuQRe8oKlxc0oF29q24liR26G.E2Yb44TkdO
17	INV0100	enoch simfukwe	marksimfukwe85@gmail.com	\N	\N	$2a$10$A1mT6D/AWZ6xSG/IoKM/oek.d2Gh.CMcyOv5.fu4hXHO8NpB7.7q2
18	INV090	enoch simfukwe	simfukweenoch@gmail.com	\N	\N	$2a$10$NABc3iCj3Ey4n68SIpEmkOthGAenmXmZSK0la3kfJ9pOtXhoGaGlu
19	INV030	twiza simfukwe	george@gmail.com	\N	\N	$2a$10$PL2a353SOkdd/STfl5x6ReGfateJhfPAqaHXPtSvR.2B001U4nzXy
\.


--
-- Data for Name: staff_role; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.staff_role (staff_id, role) FROM stdin;
1	ADMINISTRATOR
2	INVIGILATOR
3	LECTURER
10	INVIGILATOR
11	LECTURER
12	INVIGILATOR
12	ADMINISTRATOR
13	INVIGILATOR
14	INVIGILATOR
15	INVIGILATOR
16	INVIGILATOR
17	INVIGILATOR
18	INVIGILATOR
19	INVIGILATOR
\.


--
-- Data for Name: student; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.student (computer_number, national_id, full_name, program, year_of_study, email, phone, photo_path, qr_token, status) FROM stdin;
CS-2023-001	NRC-100001	Alice Student	BSc Computer Science	3	alice@student.unza.zm	+260977000010	/photos/cs-2023-001.jpg	QR-CS-2023-001	ACTIVE
CS-2023-002	NRC-100002	Bob Student	BSc Computer Science	3	bob@student.unza.zm	+260977000011	/photos/cs-2023-002.jpg	QR-CS-2023-002	ACTIVE
CS-2023-003	NRC-100003	Charlie Suspended	BSc Computer Science	2	charlie@student.unza.zm	+260977000012	/photos/cs-2023-003.jpg	QR-CS-2023-003	SUSPENDED
CS-2023-004	NRC-100004	Diana Default	BSc Information Systems	1	diana@student.unza.zm	+260977000013	/photos/cs-2023-004.jpg	QR-CS-2023-004	ACTIVE
CS-2023-005	NRC-100005	Eve Deferred	BSc Computer Engineering	2	eve@student.unza.zm	+260977000014	/photos/cs-2023-005.jpg	QR-CS-2023-005	DEFERRED
CS-2023-006	NRC-100006	Frank Graduated	BSc Computer Science	4	frank@student.unza.zm	+260977000015	/photos/cs-2023-006.jpg	QR-CS-2023-006	GRADUATED
C1783950799616	12345678	Test Student	BSc Computer Science	2	student@unza.zm	+260977000099	/photos/student.jpg	qr-C1783950799616-token	ACTIVE
\.


--
-- Data for Name: student_registration; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.student_registration (computer_number, course_code, academic_year, semester) FROM stdin;
\.


--
-- Data for Name: student_venue_allocation; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.student_venue_allocation (computer_number, exam_session_id, venue_id, seat_number) FROM stdin;
CS-2023-001	4	1	A1
\.


--
-- Data for Name: venue; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.venue (venue_id, venue_name, building, capacity) FROM stdin;
1	Great East Hall	Main Campus	200
2	Lecture Theatre 1	School of Engineering	80
3	CS Lab 1	Computer Science Block	30
4	Room 101	Education Block	25
5	Great East Hall	Main Campus	100
6	Lecture Theatre 1	School of Engineering	50
7	Main Hall	Block A	50
8	Main Hall	Block A	50
9	Main Hall	Block A	50
10	Main Hall	Block A	50
11	Main Hall	Block A	50
12	Main Hall	Block A	50
13	Main Hall	Block A	50
14	Main Hall	Block A	50
15	Main Hall	Block A	50
\.


--
-- Name: attendance_record_attendance_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.attendance_record_attendance_id_seq', 1, true);


--
-- Name: exam_session_exam_session_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.exam_session_exam_session_id_seq', 4, true);


--
-- Name: incidence_incidence_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.incidence_incidence_id_seq', 1, false);


--
-- Name: incident_report_incident_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.incident_report_incident_id_seq', 3, true);


--
-- Name: staff_staff_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.staff_staff_id_seq', 19, true);


--
-- Name: venue_venue_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.venue_venue_id_seq', 15, true);


--
-- Name: attendance_record attendance_record_computer_number_exam_session_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.attendance_record
    ADD CONSTRAINT attendance_record_computer_number_exam_session_id_key UNIQUE (computer_number, exam_session_id);


--
-- Name: attendance_record attendance_record_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.attendance_record
    ADD CONSTRAINT attendance_record_pkey PRIMARY KEY (attendance_id);


--
-- Name: course_lecturer course_lecturer_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.course_lecturer
    ADD CONSTRAINT course_lecturer_pkey PRIMARY KEY (course_code, staff_id);


--
-- Name: course course_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.course
    ADD CONSTRAINT course_pkey PRIMARY KEY (course_code);


--
-- Name: exam_session exam_session_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.exam_session
    ADD CONSTRAINT exam_session_pkey PRIMARY KEY (exam_session_id);


--
-- Name: exam_venue exam_venue_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.exam_venue
    ADD CONSTRAINT exam_venue_pkey PRIMARY KEY (exam_session_id, venue_id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: incidence incidence_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incidence
    ADD CONSTRAINT incidence_pkey PRIMARY KEY (incidence_id);


--
-- Name: incident_report incident_report_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident_report
    ADD CONSTRAINT incident_report_pkey PRIMARY KEY (incident_id);


--
-- Name: invigilator_assignment invigilator_assignment_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.invigilator_assignment
    ADD CONSTRAINT invigilator_assignment_pkey PRIMARY KEY (exam_session_id, venue_id, staff_id);


--
-- Name: staff staff_email_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.staff
    ADD CONSTRAINT staff_email_key UNIQUE (email);


--
-- Name: staff staff_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.staff
    ADD CONSTRAINT staff_pkey PRIMARY KEY (staff_id);


--
-- Name: staff_role staff_role_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.staff_role
    ADD CONSTRAINT staff_role_pkey PRIMARY KEY (staff_id, role);


--
-- Name: staff staff_staff_no_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.staff
    ADD CONSTRAINT staff_staff_no_key UNIQUE (staff_no);


--
-- Name: student student_email_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.student
    ADD CONSTRAINT student_email_key UNIQUE (email);


--
-- Name: student student_national_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.student
    ADD CONSTRAINT student_national_id_key UNIQUE (national_id);


--
-- Name: student student_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.student
    ADD CONSTRAINT student_pkey PRIMARY KEY (computer_number);


--
-- Name: student_registration student_registration_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.student_registration
    ADD CONSTRAINT student_registration_pkey PRIMARY KEY (computer_number, course_code, academic_year, semester);


--
-- Name: student_venue_allocation student_venue_allocation_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.student_venue_allocation
    ADD CONSTRAINT student_venue_allocation_pkey PRIMARY KEY (computer_number, exam_session_id);


--
-- Name: venue venue_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.venue
    ADD CONSTRAINT venue_pkey PRIMARY KEY (venue_id);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: idx_attendance_session; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_attendance_session ON public.attendance_record USING btree (exam_session_id);


--
-- Name: idx_attendance_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_attendance_status ON public.attendance_record USING btree (status);


--
-- Name: idx_exam_session_course; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_exam_session_course ON public.exam_session USING btree (course_code, academic_year, semester);


--
-- Name: idx_incidence_exam; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_incidence_exam ON public.incidence USING btree (exam_session_id);


--
-- Name: idx_incidence_venue; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_incidence_venue ON public.incidence USING btree (venue_id);


--
-- Name: idx_incident_session; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_incident_session ON public.incident_report USING btree (exam_session_id);


--
-- Name: idx_registration_course; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_registration_course ON public.student_registration USING btree (course_code, academic_year, semester);


--
-- Name: attendance_record attendance_record_check_in_venue_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.attendance_record
    ADD CONSTRAINT attendance_record_check_in_venue_id_fkey FOREIGN KEY (check_in_venue_id) REFERENCES public.venue(venue_id) ON DELETE RESTRICT;


--
-- Name: attendance_record attendance_record_computer_number_exam_session_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.attendance_record
    ADD CONSTRAINT attendance_record_computer_number_exam_session_id_fkey FOREIGN KEY (computer_number, exam_session_id) REFERENCES public.student_venue_allocation(computer_number, exam_session_id);


--
-- Name: attendance_record attendance_record_verified_by_staff_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.attendance_record
    ADD CONSTRAINT attendance_record_verified_by_staff_id_fkey FOREIGN KEY (verified_by_staff_id) REFERENCES public.staff(staff_id) ON DELETE RESTRICT;


--
-- Name: course_lecturer course_lecturer_course_code_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.course_lecturer
    ADD CONSTRAINT course_lecturer_course_code_fkey FOREIGN KEY (course_code) REFERENCES public.course(course_code) ON DELETE CASCADE;


--
-- Name: course_lecturer course_lecturer_staff_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.course_lecturer
    ADD CONSTRAINT course_lecturer_staff_id_fkey FOREIGN KEY (staff_id) REFERENCES public.staff(staff_id) ON DELETE RESTRICT;


--
-- Name: exam_session exam_session_course_code_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.exam_session
    ADD CONSTRAINT exam_session_course_code_fkey FOREIGN KEY (course_code) REFERENCES public.course(course_code) ON DELETE CASCADE;


--
-- Name: exam_venue exam_venue_exam_session_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.exam_venue
    ADD CONSTRAINT exam_venue_exam_session_id_fkey FOREIGN KEY (exam_session_id) REFERENCES public.exam_session(exam_session_id) ON DELETE CASCADE;


--
-- Name: exam_venue exam_venue_venue_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.exam_venue
    ADD CONSTRAINT exam_venue_venue_id_fkey FOREIGN KEY (venue_id) REFERENCES public.venue(venue_id) ON DELETE RESTRICT;


--
-- Name: incidence incidence_reported_by_staff_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incidence
    ADD CONSTRAINT incidence_reported_by_staff_id_fkey FOREIGN KEY (reported_by_staff_id) REFERENCES public.staff(staff_id);


--
-- Name: incidence incidence_venue_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incidence
    ADD CONSTRAINT incidence_venue_id_fkey FOREIGN KEY (venue_id) REFERENCES public.venue(venue_id);


--
-- Name: incident_report incident_report_computer_number_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident_report
    ADD CONSTRAINT incident_report_computer_number_fkey FOREIGN KEY (computer_number) REFERENCES public.student(computer_number) ON DELETE SET NULL;


--
-- Name: incident_report incident_report_exam_session_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident_report
    ADD CONSTRAINT incident_report_exam_session_id_fkey FOREIGN KEY (exam_session_id) REFERENCES public.exam_session(exam_session_id) ON DELETE CASCADE;


--
-- Name: incident_report incident_report_reported_by_staff_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident_report
    ADD CONSTRAINT incident_report_reported_by_staff_id_fkey FOREIGN KEY (reported_by_staff_id) REFERENCES public.staff(staff_id) ON DELETE RESTRICT;


--
-- Name: invigilator_assignment invigilator_assignment_exam_session_id_venue_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.invigilator_assignment
    ADD CONSTRAINT invigilator_assignment_exam_session_id_venue_id_fkey FOREIGN KEY (exam_session_id, venue_id) REFERENCES public.exam_venue(exam_session_id, venue_id) ON DELETE CASCADE;


--
-- Name: invigilator_assignment invigilator_assignment_staff_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.invigilator_assignment
    ADD CONSTRAINT invigilator_assignment_staff_id_fkey FOREIGN KEY (staff_id) REFERENCES public.staff(staff_id) ON DELETE RESTRICT;


--
-- Name: staff_role staff_role_staff_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.staff_role
    ADD CONSTRAINT staff_role_staff_id_fkey FOREIGN KEY (staff_id) REFERENCES public.staff(staff_id) ON DELETE CASCADE;


--
-- Name: student_registration student_registration_computer_number_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.student_registration
    ADD CONSTRAINT student_registration_computer_number_fkey FOREIGN KEY (computer_number) REFERENCES public.student(computer_number) ON DELETE CASCADE;


--
-- Name: student_registration student_registration_course_code_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.student_registration
    ADD CONSTRAINT student_registration_course_code_fkey FOREIGN KEY (course_code) REFERENCES public.course(course_code) ON DELETE CASCADE;


--
-- Name: student_venue_allocation student_venue_allocation_computer_number_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.student_venue_allocation
    ADD CONSTRAINT student_venue_allocation_computer_number_fkey FOREIGN KEY (computer_number) REFERENCES public.student(computer_number) ON DELETE CASCADE;


--
-- Name: student_venue_allocation student_venue_allocation_exam_session_id_venue_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.student_venue_allocation
    ADD CONSTRAINT student_venue_allocation_exam_session_id_venue_id_fkey FOREIGN KEY (exam_session_id, venue_id) REFERENCES public.exam_venue(exam_session_id, venue_id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

\unrestrict 8dwg92qoVXDfZeS5hpwvnPTrVGua4M0zfEg1sL2f1CpPbeKwOPcNSyBmTzsbogu

