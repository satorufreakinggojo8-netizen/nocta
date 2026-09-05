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
  estimatedSleepTime, wakeTime, nightAwakenings, sleepQuality (1-5),
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

All tables carry `createdAt`/`updatedAt`. Foreign keys cascade on user
deletion (supports the Profile → Delete Account requirement). Indexes on
`(userId, date)` for SleepSession/SleepLog since almost every query filters
by user and a date range.
