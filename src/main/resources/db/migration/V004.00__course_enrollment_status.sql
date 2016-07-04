create table course_enrollment_status (
  id bigint primary key,
  course_realisation_id bigint not null,
  student_enrollments text,
  teacher_enrollments text,
  created timestamp not null
);

create sequence course_enrollment_status_id_seq;
