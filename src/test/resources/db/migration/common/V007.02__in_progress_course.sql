insert into course(id, realisation_id, moodle_id, created, modified, import_status)
values (nextval('course_id_seq'), 1000, 1000, now() - 1080000, now() - 1080000, 'IN_PROGRESS');

insert into course(id, realisation_id, moodle_id, created, modified, import_status)
values (nextval('course_id_seq'), 1001, 1001, now(), now() - 1080000,'IN_PROGRESS');