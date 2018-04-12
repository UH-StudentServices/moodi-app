create unique index course_realisation_id_index ON course(realisation_id);
create unique index course_moodle_id_index ON course(moodle_id);

alter table course_enrollment_status add column course_id bigint not null;
alter table course_enrollment_status add constraint course_enrollment_status_course_id foreign key (course_id) references course on delete cascade;


