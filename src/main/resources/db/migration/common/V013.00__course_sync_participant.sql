create table course_sync_participant (
  id bigint primary key,
  course_id bigint not null,
  username varchar(255) not null,
  moodle_user_id bigint,
  is_student boolean default false not null,
  is_enrolled_student boolean default false not null,
  is_teacher boolean default false not null,
  created timestamp not null,
  modified timestamp not null,
  constraint course_sync_participant_course_fk foreign key (course_id) references course on delete cascade,
  constraint course_sync_participant_uq unique (course_id, username)
);

create sequence course_sync_participant_id_seq;

create index course_sync_participant_course_idx on course_sync_participant (course_id);

-- Table created in V004 was never mapped to an entity and has been unused since.
drop table if exists course_enrollment_status;
drop sequence if exists course_enrollment_status_id_seq;
