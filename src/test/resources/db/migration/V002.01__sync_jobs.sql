insert into synchronization_job_run(id, message, status, type, started, completed)
values (nextval('synchronization_job_run_id_seq'), 'Message', 'COMPLETED_SUCCESS', 'FULL', dateadd('DAY', -1, now()), dateadd('DAY', -1, now()));

insert into synchronization_job_run(id, message, status, type, started, completed)
values (nextval('synchronization_job_run_id_seq'), 'Message', 'COMPLETED_FAILURE', 'FULL', now(), now());

insert into synchronization_job_run(id, message, status, type, started, completed)
values (nextval('synchronization_job_run_id_seq'), 'Message', 'COMPLETED_SUCCESS', 'INCREMENTAL', dateadd('DAY', -1, now()), dateadd('DAY', -1, now()));

insert into synchronization_job_run(id, message, status, type, started, completed)
values (nextval('synchronization_job_run_id_seq'), 'Message', 'COMPLETED_FAILURE', 'INCREMENTAL', now(), now());