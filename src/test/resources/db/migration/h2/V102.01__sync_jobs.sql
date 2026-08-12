insert into synchronization_job_run(id, message, status, type, started, completed)
values (nextval('synchronization_job_run_id_seq'), 'Message', 'COMPLETED_SUCCESS', 'DELTA', dateadd('DAY', -1, now()), dateadd('DAY', -1, now()));

insert into synchronization_job_run(id, message, status, type, started, completed)
values (nextval('synchronization_job_run_id_seq'), 'Message', 'COMPLETED_FAILURE', 'DELTA', now(), now());