-- Drop table

-- DROP TABLE bpd_citizen;

CREATE TABLE bpd_citizen (
	fiscal_code_s varchar(16) NOT NULL,
	payoff_instr_s varchar(27) NULL,
	payoff_instr_type_c varchar(4) NULL,
	timestamp_tc_t timestamp NOT NULL,
	insert_date_t timestamp NULL,
	insert_user_s varchar(40) NULL,
	update_date_t timestamp NULL,
	update_user_s varchar(40) NULL,
	enabled_b boolean NULL,
	CONSTRAINT pk_bpd_citizen PRIMARY KEY (fiscal_code_s)
);
