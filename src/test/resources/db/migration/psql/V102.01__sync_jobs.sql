insert into synchronization_job_run(id, message, status, type, started, completed)
values (nextval('synchronization_job_run_id_seq'), 'Message', 'COMPLETED_SUCCESS', 'DELTA', now() - interval '1 day', now() - interval '1 day');

insert into synchronization_job_run(id, message, status, type, started, completed)
values (nextval('synchronization_job_run_id_seq'), 'Message', 'COMPLETED_FAILURE', 'DELTA', now(), now());
