alter sequence course_id_seq restart with (select coalesce(max(id), 0) from course) + 1 increment by 50;
alter sequence synchronization_job_run_id_seq restart with (select coalesce(max(id), 0) from synchronization_job_run) + 1 increment by 50;
alter sequence course_enrollment_status_id_seq restart with (select coalesce(max(id), 0) from course_enrollment_status) + 1 increment by 50;
alter sequence sync_lock_id_seq restart with (select coalesce(max(id), 0) from sync_lock) + 1 increment by 50;