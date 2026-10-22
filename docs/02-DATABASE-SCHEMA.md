# Database Schema (PostgreSQL via Prisma)

See `backend/prisma/schema.prisma` for the source of truth — this is the
narrative version.

- **User** — id, email, passwordHash, name, timezone, createdAt.
- **SleepGoal** — userId, goalType (enum: SLEEP_LONGER, FALL_ASLEEP_FASTER,
  WAKE_EASIER, CONSISTENCY, RECOVERY, ROUTINE, REDUCE_SCREENS), createdAt.
- **SleepProfile** — userId (1:1), typical bedtime/wake time, target duration,
  baseline onboarding answers (caffeine, exercise timing, screen use, etc.),
  computed at onboarding, editable later.
- **SleepSession** — one row per night: userId, date, bedtime, sleepAttemptTime,
  estimatedSleepTime, wakeTime, nightAwakenings, awakeMinutes, sleepQuality (1-5),
  morningEnergy (1-5), source (enum: SELF_REPORTED, HEALTH_CONNECT, WEARABLE),
  notes.
- **SleepLog** — free-form habit log entries tied to a session or day: caffeine
  time, exercise time, screen use before bed, alcohol, meal timing — kept
  separate from SleepSession so the environment/lifestyle inputs can grow
  without migrating the core sleep table.
- **SleepRoutine** — userId, name, ordered list of RoutineStep (json or child
  table), isActive, reminderTime.
- **DailyInsight** / **WeeklyInsight** — precomputed, cached summaries
  (average duration, consistency score, generated observation strings) so the
  Insights screen doesn't recompute on every load.
- **AIConversation** — userId, startedAt, title (first message excerpt).
- **AIMessage** — conversationId, role (user/assistant), content, createdAt,
  a `groundedInDataRange` field recording which date range of sleep data the
  assistant's answer was based on (for auditability, per the "never invent
  data" requirement).
- **Notification** — userId, type, title, body, scheduledFor, sentAt, read.
- **UserSettings** — userId (1:1), notification toggles, AI settings (data
  sharing scope), theme preference, units.

## Android Room (on-device) — version 5

Defined in `app/src/main/java/com/nocta/app/data/local`; this is the only store
for user data the app writes.

| Table | Added | Purpose |
|---|---|---|
| `sleep_sessions` | v1 | Normalized night log (offline-first, `synced` flag) |
| `ai_messages` | v1 | Vela conversation history |
| `sleep_tracking_sessions` | v2 | Start/stop tracking; null `endedAtEpochMillis` = active |
| `morning_check_ins` | v3 | Quality, awakenings, awake minutes, disturbance |
| `user_profile` | v4 | Age (drives age-based targets) |
| `journal_entries` | v5 | Optional daily journal, PK = sleep date epoch day, every answer nullable |
| `wind_down_runs` | v5 | Routine runs with pause bookkeeping, outcome and ticked steps |

v5 is purely additive (`MIGRATION_4_5`); no existing table is touched. A v4
database keeps every sleep record, check-in and chat message. Journal fields are
nullable on purpose: `null` means "not answered" and is distinct from `0`.
Trends and coach context never impute an unanswered field.

Prisma tables below are backend storage — they are described for completeness,
but no backend deployment or migration is part of this repo's verified state.
`SleepSession.awakeMinutes` was added to the Prisma model and API contract. A
database migration must be generated, reviewed, and applied before deploying
this backend change. No production database has been changed.
