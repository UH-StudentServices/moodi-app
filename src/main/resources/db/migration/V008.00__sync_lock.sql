create table sync_lock (
  id bigint primary key,
  course_id bigint not null,
  created timestamp not null default now(),
  reason text,
  active boolean default true,
  constraint sync_lock_course_fk foreign key (course_id) references course on delete cascade
);

create sequence sync_lock_id_seq;