# Moodi Synchronization Documentation

## Overview

Moodi synchronizes course enrollments from Sisu (University of Helsinki's study registry) to Moodle.
Sisu is the master data source; Moodle is the target. Synchronization covers all courses that Moodi
has created in Moodle (rows in the `course` table with `import_status = 'COMPLETED'` and a
`moodle_id`).

The scheduled **DELTA** synchronization resolves the needed Moodle actions by
comparing the current Sisu state against a locally stored snapshot of the last successfully synced
Sisu state (table `course_sync_participant`). The live Moodle enrollment state is **not** read
during a DELTA run for bootstrapped courses. Courses whose snapshots have never been seeded
(`course.sync_bootstrapped = false`) are automatically reconciled against the live Moodle state as
part of the same run, so no manual bootstrap step is needed. A separate manual **RECONCILE** run
compares all (or a single) course against the live Moodle state and rewrites the snapshots; it is
used to correct drift.

## Design rationale

### Context

Previously every scheduled run (then called FULL) fetched the complete enrollment list of every course from Moodle
(`core_enrol_get_enrolled_users`) and diffed the current Sisu state against the live Moodle state.
This had two problems:

- **API load**: a full Moodle enrollment read for every course on every run, plus idempotent
  re-enrollment/role calls whenever the Moodle state differed even trivially, regardless of whether
  anything had changed in Sisu.
- **Manual changes were clobbered**: any user in Moodle who carried the Moodi sync role but was
  absent or non-enrolled in Sisu was suspended and stripped of the student role, so manual
  enrollments and removals done by teachers or admins in Moodle were overwritten on the next run.

### Decision

Store the last successfully synced Sisu state per course and user in the
`course_sync_participant` table, and resolve DELTA synchronization actions purely from the
difference between the current Sisu state and that snapshot
(`DeltaUserSynchronizationActionResolver`). Keep the old live-state comparison available as the
manual RECONCILE synchronization type, which also rewrites the snapshots.

### Consequences

- Unchanged courses generate no Moodle enrollment traffic; a DELTA run over a quiet period is
  nearly free on the Moodle side.
- Manual enrollments and removals in Moodle are invisible to DELTA runs and are left untouched.
- Snapshot rows are only written after the user's Moodle actions succeed, so failures are retried
  on the next run.
- Accepted trade-offs:
  - **Drift is invisible to DELTA runs.** If someone manually strips a role or removes an
    enrollment in Moodle, DELTA will not re-assert it. Run a RECONCILE to detect and correct drift.
  - **A manually removed Moodle enrollment can be re-created in suspended state** by a suspend
    action (suspending re-creates the enrollment row, invisible to the user).
- **Bootstrap is automatic.** The `course.sync_bootstrapped` flag (migration
  `V015.00__course_sync_bootstrapped.sql`, backfilled to `true` for courses that already have
  snapshot rows) records whether a course's snapshots have been seeded. A scheduled DELTA run
  stamps RECONCILE on any non-bootstrapped course and reconciles it against the live Moodle state
  within the same run; on success the flag is set and later runs are pure delta. The flag is also
  set by import seeding and successful UNLOCK runs, and cleared when a course is removed.

## Synchronization types

`SynchronizationType` (`isDeltaBased()` is true for DELTA only):

### 1. DELTA (scheduled)
- **Schedule**: 30 minutes after the previous run completes (`DeltaSynchronizationJob`,
  `fixedDelay = 1800000`, initial delay 60 s)
- **Enabled by**: `synchronize.DELTA.enabled`. Yml defaults: enabled in the base
  `application.yml` (inherited by prod), disabled in the dev profile. Deployments override this
  per environment through the `SYNCHRONIZE_DELTA_ENABLED` environment variable, set in the
  systemd unit by moodi-infra (`synchronize_delta_enabled` host variable; OS env vars take
  precedence over the ymls).
- **Scope**: all completed courses with a Moodle ID
- **Comparison**: current Sisu state vs. `course_sync_participant` snapshot
- **Mixed runs**: courses with `sync_bootstrapped = false` are stamped RECONCILE per item and
  synchronized live-state within the same run (see RECONCILE below); the Moodle enrollment
  prefetch is narrowed to those courses only.

### 2. UNLOCK (manual, live-state)
- **Trigger**: `POST /api/v1/synchronization/unlock`
- **Scope**: courses that were locked due to errors or threshold violations; unlocks and
  re-synchronizes them against the live Moodle state, then writes snapshots.

### 3. RECONCILE (live-state)
- **Trigger**: `POST /api/v1/synchronization/reconcile` (all courses), or automatically per course
  within a scheduled DELTA run for courses with `sync_bootstrapped = false`. A single course can
  be marked for reconcile with `POST /api/v1/synchronization/reconcile/{realisationId}`, which
  clears its bootstrap flag; the next scheduled run then reconciles it.
- **Scope**: same course set as DELTA
- **Comparison**: current Sisu state vs. **live Moodle enrollment state** (the pre-delta scheduled-sync
  behavior), after which the snapshots are rewritten from the reconciled state and the course is
  marked bootstrapped.
- **Used for**: seeding the snapshots (automatic) and correcting drift caused by manual changes in
  Moodle. There is no scheduled full-set RECONCILE job; consider running one manually after larger
  manual interventions in Moodle.

## The sync state snapshot: `course_sync_participant`

One row per course and username (unique constraint `(course_id, username)`), created in migration
`V013.00__course_sync_participant.sql` (which also dropped the unused `course_enrollment_status`
table from V004):

| Column | Meaning |
|--------|---------|
| `course_id` | FK to `course`, `on delete cascade` |
| `username` | University username used to match Sisu and Moodle users |
| `moodle_user_id` | Cached Moodle user id; nullable, never overwritten with null once known |
| `is_student` | User was a student in Sisu at last successful sync |
| `is_enrolled_student` | ...with an enrolled/approved enrollment |
| `is_teacher` | User was a teacher in Sisu at last successful sync |

### Snapshot lifecycle

1. **Course import** (`EnrollmentService.writeSyncSnapshots`): rows are written for the Sisu
   students and teachers that were successfully enrolled during import. The course creator is
   excluded — Moodi enrolls the creator, but the Sisu synchronization does not manage them.
   Without this, a Sisu change between import and the first DELTA run would be invisible to the
   delta resolver. A side effect: newly imported courses do not cause a re-enrollment burst on
   their first DELTA run.
2. **Successful sync** (`SynchronizingProcessor.persistSnapshots`): after processing, the current
   Sisu state is persisted for every user whose Moodle actions all succeeded (or who needed
   none). Rows whose state is unchanged are skipped. Users with failed actions keep their old
   snapshot, so the same delta is retried on the next run. Users found in Moodle but present in
   neither Sisu nor the snapshot are considered manually managed and get no row.
3. **RECONCILE/UNLOCK**: live-state runs rewrite the snapshots from the reconciled state.
4. **Course removal**: snapshot rows are deleted explicitly when a course is marked removed, and
   by FK cascade if a course row is hard-deleted.

### The bootstrap flag: `course.sync_bootstrapped`

Records whether the snapshots for a course have been seeded. Set to `true` on successful import
seeding and after a successful live-state (RECONCILE/UNLOCK) synchronization of the course;
cleared when a course is marked removed (alongside the snapshot deletion) and by
`POST /api/v1/synchronization/reconcile/{realisationId}`. Courses with `sync_bootstrapped = false`
are automatically stamped RECONCILE within the scheduled DELTA run. A failed reconcile leaves the
flag `false`, so the bootstrap is retried on the next run.

## DELTA synchronization process

```
DeltaSynchronizationJob.execute() → SynchronizationService.synchronize(DELTA)
```

1. **Pre-checks**: refuses to start if another synchronization is running
   (`SynchronizationInProgressException`); creates a `SynchronizationJobRun` record.
2. **Course loading**: `courseService.findAllCompletedWithMoodleId()`. Each course becomes a
   `SynchronizationItem`; courses with `sync_bootstrapped = false` are stamped RECONCILE
   (auto-bootstrap), the rest DELTA.
3. **Enrichment** (`EnricherService`): Sisu course data (realisation, enrollments, teachers,
   groups) is fetched and batched as before. Moodle *course* data is prefetched for all items,
   but Moodle *enrollments and users* are only prefetched for the live-state (RECONCILE) items —
   a pure DELTA run skips them entirely.
4. **Delta resolution** (`SynchronizingProcessor.synchronizeUsersDelta`): items are built from
   Sisu students and teachers merged by username; users present in the snapshot but no longer in
   Sisu are added so their removal propagates. `DeltaUserSynchronizationActionResolver` then
   compares each user's current Sisu state against the snapshot:
   - **No snapshot row** → one idempotent `ADD_ENROLLMENT` with all applicable roles
     (student/teacher + sync role). A non-enrolled Sisu student produces no action, only a
     snapshot row.
   - **Became enrolled** → `REACTIVATE_ENROLLMENT` (re-enrolling clears the Moodle suspension
     flag).
   - **Lost enrollment** → `REMOVE_ROLES` (student role); additionally `SUSPEND_ENROLLMENT` if
     the user is not and was not a teacher. Teachers are never suspended.
   - **Became teacher** → `ADD_ENROLLMENT` (teacher + sync role).
   - **Stopped being teacher** → no action; the teacher role is intentionally never removed
     (same policy as the live-state resolver). Only the snapshot records the change.
5. **Moodle user id resolution**: taken from the snapshot when known; only users with pending
   actions and no cached id trigger a Moodle lookup by username. Users not found in Moodle are
   marked `MOODLE_USER_NOT_FOUND` and get no snapshot row (retried on every run).
6. **Threshold check** (see below), then actions are executed against Moodle in batches.
7. **Snapshot persistence** for successful items, enrollment completion, summary, logging.

## Safety thresholds

Configured per action type under `syncTresholds` (note the historical spelling) in
`application.yml`:

```yaml
syncTresholds:
  REMOVE_ROLES:
    preventAll: 5
  SUSPEND_ENROLLMENT:
    preventAll: 5
```

- `limit`: lock the course if the absolute action count reaches the limit (not currently set).
- `preventAll`: lock the course if the action would hit *every* affected user (and at least
  `preventAll` users are affected).

In delta runs (`checkDeltaThresholdLimits`) the action counts are compared against the **snapshot
population** — enrolled students for `SUSPEND_ENROLLMENT`, students for other actions — because
the current Sisu counts are exactly what is suspect when Sisu returns bad data. An empty Sisu
enrollment list therefore locks the course instead of suspending everyone. A threshold violation
throws before snapshots are persisted, so a locked course keeps its pre-run snapshot state.

## HTTP API

All endpoints require basic auth (see `AuthorizingInterceptor`).

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/v1/synchronization/status?type=<TYPE>` | GET | Status of the latest run of the given type |
| `/api/v1/synchronization/delta` | POST | Trigger a DELTA run |
| `/api/v1/synchronization/unlock` | POST | Re-sync and unlock locked courses (live-state) |
| `/api/v1/synchronization/reconcile` | POST | Live-state reconcile of all courses, rewrites snapshots |
| `/api/v1/synchronization/reconcile/{realisationId}` | POST | Mark one course for reconcile on the next scheduled run |

## Error handling

- **Lock mechanism**: courses failing thresholds or processing are locked and skipped by
  subsequent runs until unlocked (UNLOCK run or manual intervention).
- **Concurrency**: only one synchronization can run at a time, tracked via
  `synchronization_job_run`.
- **Failure recovery**: individual course failures do not abort the run; per-user Moodle failures
  leave the user's snapshot untouched, so the action is retried on the next DELTA run.

## Operations

- **Deploying the FULL → DELTA rename**: anything calling `POST /api/v1/synchronization/full`
  or `GET .../status?type=FULL` (e.g. monitoring) must switch to `/delta` and `?type=DELTA`;
  the old values now return 404/400. Migration `V014.00__rename_sync_types.sql` renames the
  existing `synchronization_job_run` rows. The scheduler is controlled per environment by the
  `SYNCHRONIZE_DELTA_ENABLED` env var from moodi-infra (`synchronize_delta_enabled` host
  variable).
- **First deployment of delta sync**: no manual bootstrap step. The first scheduled DELTA run
  auto-reconciles every course whose snapshots have not been seeded — a one-time live-state pass
  comparable in Moodle load to one pre-delta FULL run. Subsequent runs are pure delta.
- **Drift correction**: `POST /api/v1/synchronization/reconcile/{realisationId}` marks a single
  course for reconcile on the next scheduled run; run a full `POST /api/v1/synchronization/reconcile`
  after manual mass-changes in Moodle, or if DELTA behavior suggests snapshots are out of step
  with reality. Note that a RECONCILE re-asserts the Sisu state over manual Moodle changes, like
  the pre-delta scheduled sync did.
- **Manually recovered courses**: `course` rows inserted by hand default to
  `sync_bootstrapped = false` and are seeded automatically by the next scheduled run.

## Monitoring

- `synchronization_job_run` records each run with status and summary.
- Key log points: job start/completion with id, per-course processing status, slow API call
  warnings, error stack traces. A DELTA run containing auto-reconciled courses logs
  "Auto-reconciling N non-bootstrapped course(s)" at INFO (course ids at DEBUG), and the JSON
  summary log records each course's effective `synchronizationType`.
- `GET /api/v1/synchronization/status?type=DELTA` reports an error if the latest run failed.
