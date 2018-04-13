alter sequence course_id_seq minvalue 0 increment by 50;
alter sequence synchronization_job_run_id_seq minvalue 0 increment by 50;
alter sequence course_enrollment_status_id_seq minvalue 0 increment by 50;
alter sequence sync_lock_id_seq minvalue 0 increment by 50;

select setval('course_id_seq', (select coalesce(max(id), 0) from course), true);
select setval('synchronization_job_run_id_seq', (select coalesce(max(id), 0) from synchronization_job_run), true);
select setval('course_enrollment_status_id_seq', (select coalesce(max(id), 0) from course_enrollment_status), true);
select setval('sync_lock_id_seq', (select coalesce(max(id), 0) from sync_lock), true);