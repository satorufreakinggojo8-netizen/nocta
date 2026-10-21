# Tracking, Health Connect & Alarm Audit (NOCTA 2.0)

Status: audit complete; implementation scope decided per Section 16 Phase E —
"implement only what can be validated". This VM has no emulator or device, so
anything requiring runtime/OS validation is documented with its constraints
instead of being shipped untested.

## 1. Sleep tracking sources (implemented)

| Source | Status | Notes |
|---|---|---|
| Manual entry | ✅ | Log Sleep screen; source `SELF_REPORTED`; validated durations |
| App start/stop tracking | ✅ | `sleep_tracking_sessions`; survives restart (null `endedAt` = active) |
| Morning check-in | ✅ | Awakenings/awake minutes recorded only when the user reports them |
| Phone-derived estimates | ◐ | Tracking = wall-clock estimate, labeled `ESTIMATED` via `confidenceLabel`; no sensor inference claimed |
| Health Connect import | ☐ | Dependency present; see §2 — needs device validation |
| Wearable | ☐ | No API available in this app today |
| Sleep stages | ☐ | Never fabricated; stage timeline only renderable if credible stage data arrives (none today) |

Efficiency is displayed only for self-reported nights (credible awake-time
input); other sources show "—" with the limitation spelled out
(`BuildNightlyReport`).

## 2. Health Connect (verified against current docs)

- Record: `SleepSessionRecord` with optional stage values (`SLEEPING`, `LIGHT`,
  `DEEP`, `REM`, `AWAKE`, …); stage list lives inside the session record in
  `1.1.0-alpha07` (the separate `SleepStageRecord` was removed).
- Permissions: `android.permission.health.READ_SLEEP` (import),
  `WRITE_SLEEP` (export). Runtime-requested via the Health Connect permission
  contract; must be explained before requesting.
- Support: Android 14+ has Health Connect built into the framework; Android
  9–13 need the Health Connect app from Google Play; the provider supports
  API 28+. Devices without it must be handled gracefully (feature hidden).
- Decision: **not implemented in this pass.** Import mapping (time zone,
  overlapping sessions, dedupe against existing Room rows, permission UX)
  requires on-device validation that this environment cannot perform. The
  `HEALTH_CONNECT` DataSource value and entity schema are already in place, so
  a future implementation slots in without a migration.

Sources:
- https://developer.android.com/health-and-fitness/health-connect/features/sleep-sessions
- https://developer.android.com/health-and-fitness/health-connect/get-started
- https://developer.android.com/jetpack/androidx/releases/health-connect

## 3. Phone-only tracking honesty

No microphone, accelerometer, or algorithmic stage inference is implemented.
An ordinary phone cannot be claimed to measure sleep stages, snoring, or
restlessness without validation studies, so Nocta does not claim it. Start/stop
tracking records wall-clock times only, labeled "Estimated · start/stop
tracking". Future sensor work would need: runtime permission with prior
explanation, on-device-only processing, no retention of raw audio, and honest
labels — this is intentionally deferred rather than half-shipped.

## 4. Alarm audit (not implemented — constraints)

Verified policy/API facts:

- Exact alarms (`setExact`, `setExactAndAllowWhileIdle`, `setAlarmClock`) on
  Android 12+ need `SCHEDULE_EXACT_ALARM` (user-revocable special access; check
  `canScheduleExactAlarms()`) or `USE_EXACT_ALARM` (install-time, not
  revocable, but Play-restricted to apps whose **core functionality** is an
  alarm clock/calendar).
- A sleep tracker with an ancillary wake alarm should not assume
  `USE_EXACT_ALARM` eligibility; it would likely need `SCHEDULE_EXACT_ALARM`
  with a denied-permission fallback to inexact `setWindow()` scheduling.
- Alarms do not survive reboot: a `RECEIVE_BOOT_COMPLETED` receiver must
  re-schedule from persisted state; `TIME_SET` and `TIMEZONE_CHANGED` require
  recomputing wall-clock trigger times.
- Notification delivery also requires `POST_NOTIFICATIONS` runtime permission
  on Android 13+.

Decision: **not implemented in this pass.** Shipping an alarm receiver without
a device to verify scheduling, snooze/dismiss actions, reboot recovery, and
permission-denied fallback would violate the "test alarm scheduling and
cancellation" requirement. The design above is the implementation plan; no
exact-alarm permissions are declared in the manifest (per Section 8: do not
request them without establishing necessity first).

Sources:
- https://developer.android.com/develop/background-work/services/alarms
- https://developer.android.com/about/versions/14/changes/schedule-exact-alarms
- https://support.google.com/googleplay/android-developer/answer/16909972

## 5. Reminders/notifications (not implemented)

Wind-down/bedtime/morning reminders need `POST_NOTIFICATIONS` + scheduling
(WorkManager or AlarmManager). Settings storage for wake time and wind-down
routines exists (the inputs reminders would use), but no scheduler is wired;
no notification permission is declared yet. Same device-validation blocker as
the alarm.

## 6. What this means for the app

Declared permissions today: `INTERNET` only. The app makes no sensor access,
requests no sensitive permission, runs no undisclosed background tracker, and
fabricates no measurements. Section 4/8 features (sensor estimation, Health
Connect import, alarms, reminders) remain future work with the constraints
above recorded for whoever implements them on a real device.
