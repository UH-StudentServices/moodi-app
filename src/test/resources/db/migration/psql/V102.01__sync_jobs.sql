insert into synchronization_job_run(id, message, status, type, started, completed)
values (nextval('synchronization_job_run_id_seq'), 'Message', 'COMPLETED_SUCCESS', 'FULL', now() - interval '1 day', now() - interval '1 day');

insert into synchronization_job_run(id, message, status, type, started, completed)
values (nextval('synchronization_job_run_id_seq'), 'Message', 'COMPLETED_FAILURE', 'FULL', now(), now());

insert into synchronization_job_run(id, message, status, type, started, completed)
values (nextval('synchronization_job_run_id_seq'), 'Message', 'COMPLETED_SUCCESS', 'INCREMENTAL', now () - interval '1 day', now() - interval '1 day');

insert into synchronization_job_run(id, message, status, type, started, completed)
values (nextval('synchronization_job_run_id_seq'), 'Message', 'COMPLETED_FAILURE', 'INCREMENTAL', now(), now());
