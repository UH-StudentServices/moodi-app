-- The FULL synchronization type was renamed to DELTA and the INCREMENTAL type (dead since
-- 2016) was removed. The type column holds enum constant names, and rows with names that no
-- longer map to a SynchronizationType constant break entity reads (e.g. the health indicator).
update synchronization_job_run set type = 'DELTA' where type = 'FULL';
delete from synchronization_job_run where type = 'INCREMENTAL';
