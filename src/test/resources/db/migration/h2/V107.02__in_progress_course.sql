insert into course(id, realisation_id, moodle_id, created, modified, import_status)
values (nextval('course_id_seq'), 1000, 1000, parsedatetime('17-09-2012 18:47:52.69', 'dd-MM-yyyy HH:mm:ss.SS'), now(), 'IN_PROGRESS');

insert into course(id, realisation_id, moodle_id, created, modified, import_status)
values (nextval('course_id_seq'), 1001, 1001, now(), now(),'IN_PROGRESS');
