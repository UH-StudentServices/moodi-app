alter table course
alter column realisation_id type varchar(128);

alter table course_enrollment_status
alter column course_realisation_id type varchar(128);
