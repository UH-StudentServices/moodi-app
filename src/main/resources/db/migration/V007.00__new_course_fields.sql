alter table course add column modified timestamp not null;
alter table course add column removed boolean default false;
alter table course add column removed_message varchar(30);


