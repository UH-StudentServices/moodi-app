alter table course add column import_status varchar(20);

update course set import_status = 'COMPLETED';

alter table course alter column import_status set not null;