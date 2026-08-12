# Moodi Synchronization Documentation

## Overview

Moodi synchronizes course enrollments from Sisu (University of Helsinki's study registry) to Moodle.
Sisu is the master data source; Moodle is the target. Synchronization covers all courses that Moodi
has created in Moodle (rows in the `course` table with `import_status = 'COMPLETED'` and a
`moodle_id`).

The scheduled **FULL** synchronization is *delta-based*: it resolves the needed Moodle actions by
comparing the current Sisu state against a locally stored snapshot of the last successfully synced
Sisu state (table `course_sync_participant`). The live Moodle enrollment state is **not** read
during a FULL run. A separate manual **RECONCILE** run compares against the live Moodle state and
rewrites the snapshots; it is used to bootstrap the snapshots and to correct drift.

## Design rationale

### Context

Previously every FULL run fetched the complete enrollment list of every course from Moodle
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
`course_sync_participant` table, and resolve FULL synchronization actions purely from the
difference between the current Sisu state and that snapshot
(`DeltaUserSynchronizationActionResolver`). Keep the old live-state comparison available as the
manual RECONCILE synchronization type, which also rewrites the snapshots.

### Consequences

- Unchanged courses generate no Moodle enrollment traffic; a FULL run over a quiet period is
  nearly free on the Moodle side.
- Manual enrollments and removals in Moodle are invisible to FULL runs and are left untouched.
- Snapshot rows are only written after the user's Moodle actions succeed, so failures are retried
  on the next run.
- Accepted trade-offs:
  - **Drift is invisible to FULL runs.** If someone manually strips a role or removes an
    enrollment in Moodle, FULL will not re-assert it. Run a RECONCILE to detect and correct drift.
  - **A manually removed Moodle enrollment can be re-created in suspended state** by a suspend
    action (suspending re-creates the enrollment row, invisible to the user).
  - **Bootstrap is an operational step, not enforced by code.** After deploying the first version
    with delta sync, `POST /api/v1/synchronization/reconcile` must be run once to seed the
    snapshots. If it is skipped, the first FULL run treats every participant as never synced and
    emits a large (but idempotent) enrollment burst plus one Moodle user lookup per user.

## Synchronization types

`SynchronizationType` (`isDeltaBased()` is true for FULL only):

### 1. FULL (scheduled, delta-based)
- **Schedule**: 30 minutes after the previous run completes (`FullSynchronizationJob`,
  `fixedDelay = 1800000`, initial delay 60 s)
- **Enabled by**: `synchronize.FULL.enabled` (enabled in production, disabled locally)
- **Scope**: all completed courses with a Moodle ID
- **Comparison**: current Sisu state vs. `course_sync_participant` snapshot

### 2. UNLOCK (manual, live-state)
- **Trigger**: `POST /api/v1/synchronization/unlock`
- **Scope**: courses that were locked due to errors or threshold violations; unlocks and
  re-synchronizes them against the live Moodle state, then writes snapshots.

### 3. RECONCILE (manual, live-state)
- **Trigger**: `POST /api/v1/synchronization/reconcile`
- **Scope**: same course set as FULL
- **Comparison**: current Sisu state vs. **live Moodle enrollment state** (the pre-delta FULL
  behavior), after which the snapshots are rewritten from the reconciled state.
- **Used for**: seeding the snapshots after deployment (bootstrap) and correcting drift caused by
  manual changes in Moodle. There is no scheduled RECONCILE job; consider running it manually
  after larger manual interventions in Moodle.

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
   Without this, a Sisu change between import and the first FULL run would be invisible to the
   delta resolver. A side effect: newly imported courses do not cause a re-enrollment burst on
   their first FULL run.
2. **Successful sync** (`SynchronizingProcessor.persistSnapshots`): after processing, the current
   Sisu state is persisted for every user whose Moodle actions all succeeded (or who needed
   none). Rows whose state is unchanged are skipped. Users with failed actions keep their old
   snapshot, so the same delta is retried on the next run. Users found in Moodle but present in
   neither Sisu nor the snapshot are considered manually managed and get no row.
3. **RECONCILE/UNLOCK**: live-state runs rewrite the snapshots from the reconciled state.
4. **Course removal**: snapshot rows are deleted explicitly when a course is marked removed, and
   by FK cascade if a course row is hard-deleted.

## FULL (delta) synchronization process

```
FullSynchronizationJob.execute() → SynchronizationService.synchronize(FULL)
```

1. **Pre-checks**: refuses to start if another synchronization is running
   (`SynchronizationInProgressException`); creates a `SynchronizationJobRun` record.
2. **Course loading**: `courseService.findAllCompletedWithMoodleId()`.
3. **Enrichment** (`EnricherService`): Sisu course data (realisation, enrollments, teachers,
   groups) is fetched and batched as before. Moodle *course* data is prefetched, but Moodle
   *enrollments and users* are only prefetched when the run contains non-delta items — a pure
   FULL run skips them entirely.
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
| `/api/v1/synchronization/full` | POST | Trigger a FULL (delta) run |
| `/api/v1/synchronization/unlock` | POST | Re-sync and unlock locked courses (live-state) |
| `/api/v1/synchronization/reconcile` | POST | Live-state reconcile, rewrites snapshots |

## Error handling

- **Lock mechanism**: courses failing thresholds or processing are locked and skipped by
  subsequent runs until unlocked (UNLOCK run or manual intervention).
- **Concurrency**: only one synchronization can run at a time, tracked via
  `synchronization_job_run`.
- **Failure recovery**: individual course failures do not abort the run; per-user Moodle failures
  leave the user's snapshot untouched, so the action is retried on the next FULL run.

## Operations

- **Bootstrap after first deployment of delta sync**: run
  `POST /api/v1/synchronization/reconcile` once. Subsequent scheduled FULL runs are pure delta.
- **Drift correction**: run a RECONCILE after manual mass-changes in Moodle, or if FULL behavior
  suggests snapshots are out of step with reality. Note that a RECONCILE re-asserts the Sisu
  state over manual Moodle changes, like the old FULL sync did.
- **Manually recovered courses**: `course` rows inserted by hand have no snapshots; a RECONCILE
  is needed to seed them.

## Monitoring

- `synchronization_job_run` records each run with status and summary.
- Key log points: job start/completion with id, per-course processing status, slow API call
  warnings, error stack traces.
- `GET /api/v1/synchronization/status?type=FULL` reports an error if the latest run failed.
