create table course (
  id bigint primary key,
  realisation_id bigint not null,
  moodle_id bigint not null,
  created timestamp not null
);

create sequence course_id_seq;