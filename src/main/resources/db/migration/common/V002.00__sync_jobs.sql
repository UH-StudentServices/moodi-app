create table synchronization_job_run (
  id bigint primary key,
  status varchar(30) not null,
  type varchar(30) not null,
  message varchar(2000) not null,
  started timestamp not null,
  completed timestamp
);

create sequence synchronization_job_run_id_seq;
