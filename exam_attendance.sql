--
-- PostgreSQL database dump
--

\restrict UbRNeonljx5S3w16kmx9o14RjeK3zmUqSzMecIA1yH4SdTGhfadXS9ct04OlSEi

-- Dumped from database version 18.6 (Ubuntu 18.6-0ubuntu0.26.04.1)
-- Dumped by pg_dump version 18.6 (Ubuntu 18.6-0ubuntu0.26.04.1)

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
-- Name: public; Type: SCHEMA; Schema: -; Owner: postgres
--

-- *not* creating schema, since initdb creates it


ALTER SCHEMA public OWNER TO postgres;

--
-- Name: SCHEMA public; Type: COMMENT; Schema: -; Owner: postgres
--

COMMENT ON SCHEMA public IS '';


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
-- Name: account_activation; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.account_activation (
    activation_id bigint NOT NULL,
    computer_number character varying(15) NOT NULL,
    token_hash character varying(64) NOT NULL,
    expires_at timestamp without time zone NOT NULL,
    used_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.account_activation OWNER TO postgres;

--
-- Name: account_activation_activation_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.account_activation_activation_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.account_activation_activation_id_seq OWNER TO postgres;

--
-- Name: account_activation_activation_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.account_activation_activation_id_seq OWNED BY public.account_activation.activation_id;


--
-- Name: attendance; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.attendance (
    attendance_id integer CONSTRAINT attendance_record_attendance_id_not_null NOT NULL,
    computer_number character varying(15) CONSTRAINT attendance_record_computer_number_not_null NOT NULL,
    exam_session_id integer CONSTRAINT attendance_record_exam_session_id_not_null NOT NULL,
    check_in_time timestamp without time zone CONSTRAINT attendance_record_check_in_time_not_null NOT NULL,
    verification_method character varying(20) CONSTRAINT attendance_record_verification_method_not_null NOT NULL,
    verified_by_staff_id integer CONSTRAINT attendance_record_verified_by_staff_id_not_null NOT NULL,
    check_in_venue_id integer CONSTRAINT attendance_record_check_in_venue_id_not_null NOT NULL,
    scripts_submitted boolean DEFAULT false CONSTRAINT attendance_record_scripts_submitted_not_null NOT NULL,
    alert_message text,
    attendance_status character varying(30) DEFAULT 'PRESENT'::character varying NOT NULL,
    CONSTRAINT attendance_status_check CHECK (((attendance_status)::text = ANY ((ARRAY['PRESENT'::character varying, 'ABSENT'::character varying, 'LATE'::character varying, 'WRONG_VENUE'::character varying])::text[]))),
    CONSTRAINT attendance_verification_method_check CHECK (((verification_method)::text = ANY ((ARRAY['COMPUTER'::character varying, 'QR_CODE'::character varying, 'FACIAL_RECOGNITION'::character varying, 'QR_AND_FACE'::character varying, 'QR_AND_FACIAL'::character varying])::text[])))
);


ALTER TABLE public.attendance OWNER TO postgres;

--
-- Name: attendance_record_attendance_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.attendance ALTER COLUMN attendance_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.attendance_record_attendance_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


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
    status character varying(20) DEFAULT 'SCHEDULED'::character varying NOT NULL,
    CONSTRAINT exam_session_check CHECK ((end_time > start_time)),
    CONSTRAINT exam_session_exam_type_check CHECK (((exam_type)::text = ANY (ARRAY[('FINAL'::character varying)::text, ('SUPPLEMENTARY'::character varying)::text, ('SPECIAL'::character varying)::text]))),
    CONSTRAINT exam_session_semester_check CHECK ((semester = ANY (ARRAY[1, 2]))),
    CONSTRAINT exam_session_status_check CHECK (((status)::text = ANY ((ARRAY['SCHEDULED'::character varying, 'IN_PROGRESS'::character varying, 'COMPLETED'::character varying])::text[])))
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
-- Name: examination_pass; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.examination_pass (
    pass_id bigint NOT NULL,
    computer_number character varying(15) NOT NULL,
    academic_year character varying(20) NOT NULL,
    semester integer NOT NULL,
    qr_token text NOT NULL,
    qr_jti character varying(64) NOT NULL,
    generated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    expires_at timestamp without time zone NOT NULL
);


ALTER TABLE public.examination_pass OWNER TO postgres;

--
-- Name: examination_pass_pass_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.examination_pass_pass_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.examination_pass_pass_id_seq OWNER TO postgres;

--
-- Name: examination_pass_pass_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.examination_pass_pass_id_seq OWNED BY public.examination_pass.pass_id;


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
-- Name: generated_report; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.generated_report (
    report_id bigint NOT NULL,
    exam_session_id integer NOT NULL,
    generated_by_staff_id integer,
    title character varying(200) NOT NULL,
    report_type character varying(50) DEFAULT 'EXAMINATION_ATTENDANCE'::character varying NOT NULL,
    file_path character varying(500) NOT NULL,
    generated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    summary jsonb
);


ALTER TABLE public.generated_report OWNER TO postgres;

--
-- Name: generated_report_report_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.generated_report_report_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.generated_report_report_id_seq OWNER TO postgres;

--
-- Name: generated_report_report_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.generated_report_report_id_seq OWNED BY public.generated_report.report_id;


--
-- Name: incident; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.incident (
    incident_id integer CONSTRAINT incident_report_incident_id_not_null NOT NULL,
    exam_session_id integer CONSTRAINT incident_report_exam_session_id_not_null NOT NULL,
    computer_number character varying(15),
    reported_by_staff_id integer CONSTRAINT incident_report_reported_by_staff_id_not_null NOT NULL,
    incident_time timestamp without time zone,
    description character varying(1000) CONSTRAINT incident_report_description_not_null NOT NULL,
    severity character varying(20) DEFAULT 'MINOR'::character varying CONSTRAINT incident_report_severity_not_null NOT NULL,
    incident_type character varying(50),
    evidence_path character varying(500),
    occurred_at timestamp without time zone NOT NULL,
    venue_id integer,
    CONSTRAINT incident_report_severity_check CHECK (((severity)::text = ANY (ARRAY[('MINOR'::character varying)::text, ('MAJOR'::character varying)::text, ('CRITICAL'::character varying)::text]))),
    CONSTRAINT incident_type_check CHECK (((incident_type)::text = ANY ((ARRAY['CHEATING'::character varying, 'PHONE_FOUND'::character varying, 'WRONG_VENUE'::character varying, 'MEDICAL_EMERGENCY'::character varying, 'DISTURBANCE'::character varying, 'LATE_ARRIVAL'::character varying, 'OTHER'::character varying])::text[])))
);


ALTER TABLE public.incident OWNER TO postgres;

--
-- Name: incident_report_incident_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.incident ALTER COLUMN incident_id ADD GENERATED ALWAYS AS IDENTITY (
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
-- Name: refresh_token; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.refresh_token (
    token_id bigint NOT NULL,
    staff_id integer NOT NULL,
    token character varying(512) NOT NULL,
    expires_at timestamp without time zone NOT NULL,
    revoked boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.refresh_token OWNER TO postgres;

--
-- Name: refresh_token_token_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.refresh_token_token_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.refresh_token_token_id_seq OWNER TO postgres;

--
-- Name: refresh_token_token_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.refresh_token_token_id_seq OWNED BY public.refresh_token.token_id;


--
-- Name: role; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.role (
    role_id integer NOT NULL,
    name character varying(50) NOT NULL
);


ALTER TABLE public.role OWNER TO postgres;

--
-- Name: role_role_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.role_role_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.role_role_id_seq OWNER TO postgres;

--
-- Name: role_role_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.role_role_id_seq OWNED BY public.role.role_id;


--
-- Name: staff; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.staff (
    staff_id integer NOT NULL,
    full_name character varying(120) NOT NULL,
    email character varying(120) NOT NULL,
    phone character varying(20),
    department character varying(120),
    password_hash character varying(255),
    account_status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    CONSTRAINT staff_account_status_check CHECK (((account_status)::text = ANY ((ARRAY['PENDING'::character varying, 'ACTIVE'::character varying, 'SUSPENDED'::character varying])::text[])))
);


ALTER TABLE public.staff OWNER TO postgres;

--
-- Name: staff_activation_token; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.staff_activation_token (
    activation_id bigint NOT NULL,
    staff_id integer NOT NULL,
    token_hash character varying(255) NOT NULL,
    expires_at timestamp without time zone NOT NULL,
    used_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.staff_activation_token OWNER TO postgres;

--
-- Name: staff_activation_token_activation_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.staff_activation_token_activation_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.staff_activation_token_activation_id_seq OWNER TO postgres;

--
-- Name: staff_activation_token_activation_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.staff_activation_token_activation_id_seq OWNED BY public.staff_activation_token.activation_id;


--
-- Name: staff_role; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.staff_role (
    staff_id integer CONSTRAINT staff_role_new_staff_id_not_null NOT NULL,
    role_id integer CONSTRAINT staff_role_new_role_id_not_null NOT NULL
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
    school character varying(120) NOT NULL,
    password_hash character varying(255),
    account_activated boolean DEFAULT false NOT NULL,
    activated_at timestamp without time zone,
    account_status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    CONSTRAINT student_account_status_check CHECK (((account_status)::text = ANY ((ARRAY['PENDING'::character varying, 'ACTIVE'::character varying, 'SUSPENDED'::character varying])::text[]))),
    CONSTRAINT student_status_check CHECK (((status)::text = ANY (ARRAY[('ACTIVE'::character varying)::text, ('SUSPENDED'::character varying)::text, ('GRADUATED'::character varying)::text, ('DEFERRED'::character varying)::text]))),
    CONSTRAINT student_year_of_study_check CHECK (((year_of_study >= 1) AND (year_of_study <= 7)))
);


ALTER TABLE public.student OWNER TO postgres;

--
-- Name: student_refresh_token; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.student_refresh_token (
    token_id bigint NOT NULL,
    computer_number character varying(15) NOT NULL,
    token character varying(512) NOT NULL,
    expires_at timestamp without time zone NOT NULL,
    revoked boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.student_refresh_token OWNER TO postgres;

--
-- Name: student_refresh_token_token_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.student_refresh_token_token_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.student_refresh_token_token_id_seq OWNER TO postgres;

--
-- Name: student_refresh_token_token_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.student_refresh_token_token_id_seq OWNED BY public.student_refresh_token.token_id;


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
-- Name: account_activation activation_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.account_activation ALTER COLUMN activation_id SET DEFAULT nextval('public.account_activation_activation_id_seq'::regclass);


--
-- Name: examination_pass pass_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.examination_pass ALTER COLUMN pass_id SET DEFAULT nextval('public.examination_pass_pass_id_seq'::regclass);


--
-- Name: generated_report report_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.generated_report ALTER COLUMN report_id SET DEFAULT nextval('public.generated_report_report_id_seq'::regclass);


--
-- Name: refresh_token token_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.refresh_token ALTER COLUMN token_id SET DEFAULT nextval('public.refresh_token_token_id_seq'::regclass);


--
-- Name: role role_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.role ALTER COLUMN role_id SET DEFAULT nextval('public.role_role_id_seq'::regclass);


--
-- Name: staff_activation_token activation_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.staff_activation_token ALTER COLUMN activation_id SET DEFAULT nextval('public.staff_activation_token_activation_id_seq'::regclass);


--
-- Name: student_refresh_token token_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.student_refresh_token ALTER COLUMN token_id SET DEFAULT nextval('public.student_refresh_token_token_id_seq'::regclass);


--
-- Data for Name: account_activation; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.account_activation (activation_id, computer_number, token_hash, expires_at, used_at, created_at) FROM stdin;
\.


--
-- Data for Name: attendance; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.attendance (attendance_id, computer_number, exam_session_id, check_in_time, verification_method, verified_by_staff_id, check_in_venue_id, scripts_submitted, alert_message, attendance_status) FROM stdin;
\.


--
-- Data for Name: course_lecturer; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.course_lecturer (course_code, staff_id) FROM stdin;
\.


--
-- Data for Name: exam_session; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.exam_session (exam_session_id, course_code, exam_date, start_time, end_time, academic_year, semester, exam_type, status) FROM stdin;
\.


--
-- Data for Name: exam_venue; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.exam_venue (exam_session_id, venue_id) FROM stdin;
\.


--
-- Data for Name: examination_pass; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.examination_pass (pass_id, computer_number, academic_year, semester, qr_token, qr_jti, generated_at, expires_at) FROM stdin;
\.


--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	1	<< Flyway Baseline >>	BASELINE	<< Flyway Baseline >>	\N	postgres	2026-07-13 21:23:21.45335	0	t
2	2	add alert message to attendance record	SQL	V2__add_alert_message_to_attendance_record.sql	2003978427	postgres	2026-07-16 17:56:23.404751	61	t
3	3	create incidence table	SQL	V3__create_incidence_table.sql	698868138	postgres	2026-07-16 18:18:34.562444	1376	t
4	4	attendance domain alignment	SQL	V4__attendance_domain_alignment.sql	2032604889	postgres	2026-08-23 07:52:10.390683	49	t
5	5	allow existing flyway history	SQL	V5__allow_existing_flyway_history.sql	-1395798189	postgres	2026-08-23 07:52:10.571026	4	t
6	6	final attendance schema	SQL	V6__final_attendance_schema.sql	1345888397	postgres	2026-08-23 07:52:10.697375	178	t
7	7	drop legacy tables	SQL	V7__drop_legacy_tables.sql	-1598673016	postgres	2026-08-23 07:52:10.91968	22	t
8	8	dummy testing data	SQL	V8__dummy_testing_data.sql	-647806936	postgres	2026-08-23 07:52:11.081215	167	t
9	9	restore operational tables	SQL	V9__restore_operational_tables.sql	1640126907	postgres	2026-08-23 07:52:11.284624	181	t
10	10	seed operational data	SQL	V10__seed_operational_data.sql	-1384555262	postgres	2026-08-23 07:52:11.495025	19	t
11	11	invigilator demo seed	SQL	V11__invigilator_demo_seed.sql	-871015976	postgres	2026-08-23 07:52:11.673529	8	t
12	12	reset demo passwords	SQL	V12__reset_demo_passwords.sql	-576408144	postgres	2026-08-23 07:52:11.849334	6	t
13	13	course lecturer for invigilators	SQL	V13__course_lecturer_for_invigilators.sql	-722643320	postgres	2026-08-23 07:52:12.067627	14	t
14	14	fix attendance status and reset demo	SQL	V14__fix_attendance_status_and_reset_demo.sql	-1674889643	postgres	2026-08-23 07:52:12.216734	13	t
15	15	reset demo attendance again	SQL	V15__reset_demo_attendance_again.sql	1179709812	postgres	2026-08-23 07:52:12.36614	6	t
16	16	reset all operational activity	SQL	V16__reset_all_operational_activity.sql	-451307943	postgres	2026-08-23 07:52:12.516879	4	t
17	17	fix invigilator schedule overlap	SQL	V17__fix_invigilator_schedule_overlap.sql	999056573	postgres	2026-08-23 07:52:12.6662	140	t
18	18	student auth and seed	SQL	V18__student_auth_and_seed.sql	-1098975968	postgres	2026-08-23 07:52:12.820168	146	t
19	19	examination slips	SQL	V19__examination_slips.sql	-1223317816	postgres	2026-08-23 07:52:12.985095	146	t
20	20	examination pass	SQL	V20__examination_pass.sql	231176624	postgres	2026-08-23 07:52:13.284066	151	t
21	21	reset all user activity	SQL	V21__reset_all_user_activity.sql	-1980306389	postgres	2026-08-23 07:52:13.451216	9	t
22	22	reset to single admin and remove staff no	SQL	V22__reset_to_single_admin_and_remove_staff_no.sql	1669567024	postgres	2026-08-23 08:00:56.746258	170	t
23	23	account activation and status	SQL	V23__account_activation_and_status.sql	270772099	postgres	2026-08-23 08:31:40.663272	87	t
24	24	drop account activation	SQL	V24__drop_account_activation.sql	-1449883204	postgres	2026-08-23 09:34:47.184997	73	t
25	25	student activation tokens	SQL	V25__student_activation_tokens.sql	1117913785	postgres	2026-08-23 10:50:09.790014	237	t
26	26	staff activation tokens	SQL	V26__staff_activation_tokens.sql	-382142910	postgres	2026-09-14 14:15:07.254563	183	t
\.


--
-- Data for Name: generated_report; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.generated_report (report_id, exam_session_id, generated_by_staff_id, title, report_type, file_path, generated_at, summary) FROM stdin;
\.


--
-- Data for Name: incident; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.incident (incident_id, exam_session_id, computer_number, reported_by_staff_id, incident_time, description, severity, incident_type, evidence_path, occurred_at, venue_id) FROM stdin;
\.


--
-- Data for Name: invigilator_assignment; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.invigilator_assignment (exam_session_id, venue_id, staff_id) FROM stdin;
\.


--
-- Data for Name: refresh_token; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.refresh_token (token_id, staff_id, token, expires_at, revoked, created_at) FROM stdin;
2	1	eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbkBnbWFpbC5jb20iLCJqdGkiOiJjZmM5NTk0NS02OWVlLTRmMjAtOWYyMi0xZmRiNDQwMWY3MDkiLCJpYXQiOjE3ODc0NzAxNTIsImV4cCI6MTc4ODA3NDk1Mn0.J5YV9XBZJJx7C0HFVRdNQdOJbefOa7KW8hGv88dOJUc	2026-08-30 09:29:12.70448	f	2026-08-23 09:29:12.704455
3	2	eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJicmlhbi5waGlyaUBleGFtcGxlLmNvbSIsImp0aSI6Ijk4ZDE4ZDQwLTdhMjMtNGEzZC1iZTAwLWYzNDUxYzdkNjcwOCIsImlhdCI6MTc4NzQ3MTQ5NywiZXhwIjoxNzg4MDc2Mjk3fQ.rzhVFo-e3BFqPWnNf2FMdk8S02ystiduj6DW8kt5zbs	2026-08-30 09:51:37.958488	f	2026-08-23 09:51:37.958471
4	2	eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJicmlhbi5waGlyaUBleGFtcGxlLmNvbSIsImp0aSI6IjZjYWYxMmNiLTVhYTgtNGM5NC1hY2YyLTEwODQ3NDg4OTA5OCIsImlhdCI6MTc4NzQ3MjA1NiwiZXhwIjoxNzg4MDc2ODU2fQ.lS5c3EX6J_9OzqzFiRcGHfkUkupFkd1W2R0jOySP-jA	2026-08-30 10:00:56.030275	f	2026-08-23 10:00:56.03026
5	2	eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJicmlhbi5waGlyaUBleGFtcGxlLmNvbSIsImp0aSI6Ijc5YzNlNWE2LWVjYWItNDUxZC1iMGEzLWIxMzU0NWQzYzUyMyIsImlhdCI6MTc4NzQ3Mjc1NCwiZXhwIjoxNzg4MDc3NTU0fQ.B55VQYH0G5Vp26auXc92s3fXC9v41pOJZ56qOfJsewk	2026-08-30 10:12:34.625005	f	2026-08-23 10:12:34.624993
6	2	eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJicmlhbi5waGlyaUBleGFtcGxlLmNvbSIsImp0aSI6ImQwNjk0YjgyLWRmMmMtNGYxYi1iY2ViLThhZWJiYzU5NzliOSIsImlhdCI6MTc4NzQ3MzE4MCwiZXhwIjoxNzg4MDc3OTgwfQ.rHhboK-N30UZ8tUl_1GdXBxMJIRGpOvpo4SyHcsPlgY	2026-08-30 10:19:40.274274	f	2026-08-23 10:19:40.274245
7	2	eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJicmlhbi5waGlyaUBleGFtcGxlLmNvbSIsImp0aSI6IjY0ZWFkOWZiLThiNGItNGViZS05ODA1LTZiMGU1OTU3MmJlZSIsImlhdCI6MTc4NzQ3NjA1MiwiZXhwIjoxNzg4MDgwODUyfQ.6Z7xeKmNs4CAN-j7kH35uoM4Ru4l15pnH9Fci360ohI	2026-08-30 11:07:32.862184	f	2026-08-23 11:07:32.862154
8	2	eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJicmlhbi5waGlyaUBleGFtcGxlLmNvbSIsImp0aSI6IjgxNTg0ZWExLTJhYzgtNGFiZC05ZWMxLWEyMDJmNTc5NTQ0MCIsImlhdCI6MTc4NzQ5MzYwOCwiZXhwIjoxNzg4MDk4NDA4fQ.V8WQRXTw6z0FY2euMRxGUYTJsIQist-SK5kNV9n8KnA	2026-08-30 16:00:08.778607	f	2026-08-23 16:00:08.778574
9	2	eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJicmlhbi5waGlyaUBleGFtcGxlLmNvbSIsImp0aSI6ImM3N2M3OGMyLTdiZDQtNGIwYy05NDIxLWVlMWUxMDQ5ZDc5YSIsImlhdCI6MTc4NzQ5NTM4MywiZXhwIjoxNzg4MTAwMTgzfQ.Nf9Jl_x9UO74Fa58ybWmFKYWedzfSRVDMU463-gzd0Y	2026-08-30 16:29:43.882556	f	2026-08-23 16:29:43.882527
10	2	eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJicmlhbi5waGlyaUBleGFtcGxlLmNvbSIsImp0aSI6IjFlNDE1NWJkLTNlZTItNGM2Yi05ZTYxLWI1NWI2NDQ0OGMxNyIsImlhdCI6MTc4NzQ5NTkxOCwiZXhwIjoxNzg4MTAwNzE4fQ.KQfd2LotgSC976MN0mvfEbjXsz55xijsjEMmLurNPhA	2026-08-30 16:38:38.547642	f	2026-08-23 16:38:38.547599
\.


--
-- Data for Name: role; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.role (role_id, name) FROM stdin;
1	ADMINISTRATOR
2	LECTURER
3	INVIGILATOR
10	STUDENT
\.


--
-- Data for Name: staff; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.staff (staff_id, full_name, email, phone, department, password_hash, account_status) FROM stdin;
1	John Simfukwe	admin@gmail.com	\N	Administration	$2a$12$he15a0vkXYkI89VVgclV..rZ85joMbPtmjSOzuB2DiOuIiW6wyob.	ACTIVE
3	Mary Banda	mary.banda@example.com	0977000002	Computer Science	\N	PENDING
4	Andrew Mulenga	andrew.mulenga@example.com	0977000003	Information Technology	\N	PENDING
2	Brian Phiri	brian.phiri@example.com	0977000001	Computer Science	$2a$12$cdRt.tU.lSqkMrTy.fcXVegkjZzY3QbEmJd1TV4JAgbliXGt0capm	ACTIVE
\.


--
-- Data for Name: staff_activation_token; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.staff_activation_token (activation_id, staff_id, token_hash, expires_at, used_at, created_at) FROM stdin;
\.


--
-- Data for Name: staff_role; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.staff_role (staff_id, role_id) FROM stdin;
1	1
3	2
2	2
4	3
\.


--
-- Data for Name: student; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.student (computer_number, national_id, full_name, program, year_of_study, email, phone, photo_path, qr_token, status, school, password_hash, account_activated, activated_at, account_status) FROM stdin;
\.


--
-- Data for Name: student_refresh_token; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.student_refresh_token (token_id, computer_number, token, expires_at, revoked, created_at) FROM stdin;
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
\.


--
-- Data for Name: venue; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.venue (venue_id, venue_name, building, capacity) FROM stdin;
\.


--
-- Name: account_activation_activation_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.account_activation_activation_id_seq', 1, false);


--
-- Name: attendance_record_attendance_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.attendance_record_attendance_id_seq', 1, false);


--
-- Name: exam_session_exam_session_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.exam_session_exam_session_id_seq', 1, false);


--
-- Name: examination_pass_pass_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.examination_pass_pass_id_seq', 1, false);


--
-- Name: generated_report_report_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.generated_report_report_id_seq', 1, false);


--
-- Name: incident_report_incident_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.incident_report_incident_id_seq', 1, false);


--
-- Name: refresh_token_token_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.refresh_token_token_id_seq', 10, true);


--
-- Name: role_role_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.role_role_id_seq', 10, true);


--
-- Name: staff_activation_token_activation_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.staff_activation_token_activation_id_seq', 1, false);


--
-- Name: staff_staff_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.staff_staff_id_seq', 4, true);


--
-- Name: student_refresh_token_token_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.student_refresh_token_token_id_seq', 1, false);


--
-- Name: venue_venue_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.venue_venue_id_seq', 1, false);


--
-- Name: account_activation account_activation_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.account_activation
    ADD CONSTRAINT account_activation_pkey PRIMARY KEY (activation_id);


--
-- Name: account_activation account_activation_token_hash_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.account_activation
    ADD CONSTRAINT account_activation_token_hash_key UNIQUE (token_hash);


--
-- Name: attendance attendance_record_computer_number_exam_session_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.attendance
    ADD CONSTRAINT attendance_record_computer_number_exam_session_id_key UNIQUE (computer_number, exam_session_id);


--
-- Name: attendance attendance_record_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.attendance
    ADD CONSTRAINT attendance_record_pkey PRIMARY KEY (attendance_id);


--
-- Name: course_lecturer course_lecturer_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.course_lecturer
    ADD CONSTRAINT course_lecturer_pkey PRIMARY KEY (course_code, staff_id);


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
-- Name: examination_pass examination_pass_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.examination_pass
    ADD CONSTRAINT examination_pass_pkey PRIMARY KEY (pass_id);


--
-- Name: examination_pass examination_pass_qr_jti_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.examination_pass
    ADD CONSTRAINT examination_pass_qr_jti_key UNIQUE (qr_jti);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: generated_report generated_report_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.generated_report
    ADD CONSTRAINT generated_report_pkey PRIMARY KEY (report_id);


--
-- Name: incident incident_report_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident
    ADD CONSTRAINT incident_report_pkey PRIMARY KEY (incident_id);


--
-- Name: invigilator_assignment invigilator_assignment_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.invigilator_assignment
    ADD CONSTRAINT invigilator_assignment_pkey PRIMARY KEY (exam_session_id, venue_id, staff_id);


--
-- Name: refresh_token refresh_token_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.refresh_token
    ADD CONSTRAINT refresh_token_pkey PRIMARY KEY (token_id);


--
-- Name: refresh_token refresh_token_token_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.refresh_token
    ADD CONSTRAINT refresh_token_token_key UNIQUE (token);


--
-- Name: role role_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.role
    ADD CONSTRAINT role_name_key UNIQUE (name);


--
-- Name: role role_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.role
    ADD CONSTRAINT role_pkey PRIMARY KEY (role_id);


--
-- Name: staff_activation_token staff_activation_token_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.staff_activation_token
    ADD CONSTRAINT staff_activation_token_pkey PRIMARY KEY (activation_id);


--
-- Name: staff_activation_token staff_activation_token_staff_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.staff_activation_token
    ADD CONSTRAINT staff_activation_token_staff_id_key UNIQUE (staff_id);


--
-- Name: staff_activation_token staff_activation_token_token_hash_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.staff_activation_token
    ADD CONSTRAINT staff_activation_token_token_hash_key UNIQUE (token_hash);


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
-- Name: staff_role staff_role_new_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.staff_role
    ADD CONSTRAINT staff_role_new_pkey PRIMARY KEY (staff_id, role_id);


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
-- Name: student_refresh_token student_refresh_token_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.student_refresh_token
    ADD CONSTRAINT student_refresh_token_pkey PRIMARY KEY (token_id);


--
-- Name: student_refresh_token student_refresh_token_token_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.student_refresh_token
    ADD CONSTRAINT student_refresh_token_token_key UNIQUE (token);


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
-- Name: examination_pass uq_examination_pass_student_period; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.examination_pass
    ADD CONSTRAINT uq_examination_pass_student_period UNIQUE (computer_number, academic_year, semester);


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
-- Name: idx_account_activation_outstanding; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_account_activation_outstanding ON public.account_activation USING btree (expires_at) WHERE (used_at IS NULL);


--
-- Name: idx_account_activation_student; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_account_activation_student ON public.account_activation USING btree (computer_number);


--
-- Name: idx_attendance_exam_session; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_attendance_exam_session ON public.attendance USING btree (exam_session_id);


--
-- Name: idx_attendance_session; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_attendance_session ON public.attendance USING btree (exam_session_id);


--
-- Name: idx_attendance_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_attendance_status ON public.attendance USING btree (attendance_status);


--
-- Name: idx_course_lecturer_course; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_course_lecturer_course ON public.course_lecturer USING btree (course_code);


--
-- Name: idx_exam_session_course; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_exam_session_course ON public.exam_session USING btree (course_code, academic_year, semester);


--
-- Name: idx_examination_pass_student; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_examination_pass_student ON public.examination_pass USING btree (computer_number);


--
-- Name: idx_generated_report_exam_session; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_generated_report_exam_session ON public.generated_report USING btree (exam_session_id);


--
-- Name: idx_generated_report_session; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_generated_report_session ON public.generated_report USING btree (exam_session_id);


--
-- Name: idx_incident_session; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_incident_session ON public.incident USING btree (exam_session_id);


--
-- Name: idx_refresh_token_staff; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_refresh_token_staff ON public.refresh_token USING btree (staff_id);


--
-- Name: idx_registration_course; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_registration_course ON public.student_registration USING btree (course_code, academic_year, semester);


--
-- Name: idx_staff_activation_token_outstanding; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_staff_activation_token_outstanding ON public.staff_activation_token USING btree (expires_at) WHERE (used_at IS NULL);


--
-- Name: idx_staff_activation_token_staff; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_staff_activation_token_staff ON public.staff_activation_token USING btree (staff_id);


--
-- Name: idx_student_refresh_token_student; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_student_refresh_token_student ON public.student_refresh_token USING btree (computer_number);


--
-- Name: account_activation account_activation_computer_number_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.account_activation
    ADD CONSTRAINT account_activation_computer_number_fkey FOREIGN KEY (computer_number) REFERENCES public.student(computer_number) ON DELETE CASCADE;


--
-- Name: attendance attendance_record_check_in_venue_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.attendance
    ADD CONSTRAINT attendance_record_check_in_venue_id_fkey FOREIGN KEY (check_in_venue_id) REFERENCES public.venue(venue_id) ON DELETE RESTRICT;


--
-- Name: attendance attendance_record_computer_number_exam_session_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.attendance
    ADD CONSTRAINT attendance_record_computer_number_exam_session_id_fkey FOREIGN KEY (computer_number, exam_session_id) REFERENCES public.student_venue_allocation(computer_number, exam_session_id);


--
-- Name: attendance attendance_record_verified_by_staff_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.attendance
    ADD CONSTRAINT attendance_record_verified_by_staff_id_fkey FOREIGN KEY (verified_by_staff_id) REFERENCES public.staff(staff_id) ON DELETE RESTRICT;


--
-- Name: examination_pass examination_pass_computer_number_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.examination_pass
    ADD CONSTRAINT examination_pass_computer_number_fkey FOREIGN KEY (computer_number) REFERENCES public.student(computer_number) ON DELETE CASCADE;


--
-- Name: course_lecturer fk_course_lecturer_staff; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.course_lecturer
    ADD CONSTRAINT fk_course_lecturer_staff FOREIGN KEY (staff_id) REFERENCES public.staff(staff_id) ON DELETE CASCADE;


--
-- Name: exam_venue fk_exam_venue_exam; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.exam_venue
    ADD CONSTRAINT fk_exam_venue_exam FOREIGN KEY (exam_session_id) REFERENCES public.exam_session(exam_session_id) ON DELETE CASCADE;


--
-- Name: exam_venue fk_exam_venue_venue; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.exam_venue
    ADD CONSTRAINT fk_exam_venue_venue FOREIGN KEY (venue_id) REFERENCES public.venue(venue_id) ON DELETE RESTRICT;


--
-- Name: generated_report fk_generated_report_staff; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.generated_report
    ADD CONSTRAINT fk_generated_report_staff FOREIGN KEY (generated_by_staff_id) REFERENCES public.staff(staff_id) ON DELETE SET NULL;


--
-- Name: incident fk_incident_venue; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident
    ADD CONSTRAINT fk_incident_venue FOREIGN KEY (venue_id) REFERENCES public.venue(venue_id) ON DELETE SET NULL;


--
-- Name: invigilator_assignment fk_invigilator_assignment_exam_venue; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.invigilator_assignment
    ADD CONSTRAINT fk_invigilator_assignment_exam_venue FOREIGN KEY (exam_session_id, venue_id) REFERENCES public.exam_venue(exam_session_id, venue_id) ON DELETE CASCADE;


--
-- Name: invigilator_assignment fk_invigilator_assignment_staff; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.invigilator_assignment
    ADD CONSTRAINT fk_invigilator_assignment_staff FOREIGN KEY (staff_id) REFERENCES public.staff(staff_id) ON DELETE RESTRICT;


--
-- Name: staff_role fk_staff_role_role; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.staff_role
    ADD CONSTRAINT fk_staff_role_role FOREIGN KEY (role_id) REFERENCES public.role(role_id) ON DELETE CASCADE;


--
-- Name: staff_role fk_staff_role_staff; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.staff_role
    ADD CONSTRAINT fk_staff_role_staff FOREIGN KEY (staff_id) REFERENCES public.staff(staff_id) ON DELETE CASCADE;


--
-- Name: student_registration fk_student_registration_student; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.student_registration
    ADD CONSTRAINT fk_student_registration_student FOREIGN KEY (computer_number) REFERENCES public.student(computer_number) ON DELETE CASCADE;


--
-- Name: generated_report generated_report_exam_session_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.generated_report
    ADD CONSTRAINT generated_report_exam_session_id_fkey FOREIGN KEY (exam_session_id) REFERENCES public.exam_session(exam_session_id) ON DELETE CASCADE;


--
-- Name: generated_report generated_report_generated_by_staff_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.generated_report
    ADD CONSTRAINT generated_report_generated_by_staff_id_fkey FOREIGN KEY (generated_by_staff_id) REFERENCES public.staff(staff_id) ON DELETE SET NULL;


--
-- Name: incident incident_report_computer_number_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident
    ADD CONSTRAINT incident_report_computer_number_fkey FOREIGN KEY (computer_number) REFERENCES public.student(computer_number) ON DELETE SET NULL;


--
-- Name: incident incident_report_exam_session_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident
    ADD CONSTRAINT incident_report_exam_session_id_fkey FOREIGN KEY (exam_session_id) REFERENCES public.exam_session(exam_session_id) ON DELETE CASCADE;


--
-- Name: incident incident_report_reported_by_staff_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incident
    ADD CONSTRAINT incident_report_reported_by_staff_id_fkey FOREIGN KEY (reported_by_staff_id) REFERENCES public.staff(staff_id) ON DELETE RESTRICT;


--
-- Name: refresh_token refresh_token_staff_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.refresh_token
    ADD CONSTRAINT refresh_token_staff_id_fkey FOREIGN KEY (staff_id) REFERENCES public.staff(staff_id) ON DELETE CASCADE;


--
-- Name: staff_activation_token staff_activation_token_staff_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.staff_activation_token
    ADD CONSTRAINT staff_activation_token_staff_id_fkey FOREIGN KEY (staff_id) REFERENCES public.staff(staff_id) ON DELETE CASCADE;


--
-- Name: student_refresh_token student_refresh_token_computer_number_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.student_refresh_token
    ADD CONSTRAINT student_refresh_token_computer_number_fkey FOREIGN KEY (computer_number) REFERENCES public.student(computer_number) ON DELETE CASCADE;


--
-- Name: student_venue_allocation student_venue_allocation_computer_number_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.student_venue_allocation
    ADD CONSTRAINT student_venue_allocation_computer_number_fkey FOREIGN KEY (computer_number) REFERENCES public.student(computer_number) ON DELETE CASCADE;


--
-- Name: SCHEMA public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE USAGE ON SCHEMA public FROM PUBLIC;


--
-- PostgreSQL database dump complete
--

\unrestrict UbRNeonljx5S3w16kmx9o14RjeK3zmUqSzMecIA1yH4SdTGhfadXS9ct04OlSEi

