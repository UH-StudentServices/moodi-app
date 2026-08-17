-- Tracks whether the course_sync_participant snapshots for a course have been seeded
-- (by course import or a successful live-state RECONCILE/UNLOCK run). Non-bootstrapped
-- courses are automatically reconciled against the live Moodle state as part of the
-- scheduled DELTA synchronization instead of being delta-synced.
alter table course add column sync_bootstrapped boolean default false not null;

-- Courses that already have snapshot rows were bootstrapped by import seeding or an
-- earlier reconcile run.
update course set sync_bootstrapped = true
where id in (select distinct course_id from course_sync_participant);
