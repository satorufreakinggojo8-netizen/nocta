# Nocta — AI Sleep Optimization App

A premium, dark-first sleep-optimization app: Android client (Kotlin
+ Jetpack Compose) and a Node/TypeScript backend that proxies an AI sleep
coach ("Vela") grounded in the user's own logged data. Fully original
branding — no relation to any existing product.

## Repo layout

```
docs/           Architecture, database schema, and screen-status docs — read first
app/            Android app (Kotlin, Jetpack Compose, Clean Architecture + MVVM)
backend/        Node/TypeScript + Express + Prisma/PostgreSQL API + AI proxy
```

Start with `docs/01-ARCHITECTURE.md`, `docs/02-DATABASE-SCHEMA.md`, and
`docs/03-SCREENS.md` for the current architecture and implementation limits.

## Implemented app features

The Android app keeps the existing dark Nocta theme and includes:

- Sleep start/stop tracking, morning check-in, and manual sleep logging.
- One shared sleep-score use case used by Home, Sleep, Insights, and Sleep Details. It scores age-aware duration, circular bedtime and midpoint regularity, self-reported quality/energy, awakenings, and awake minutes from up to 14 logged nights.
- A recovery/Insights page with user-entered caffeine, alcohol, naps, movement, stress, daytime function, and morning/afternoon/evening energy logs.
- A persistent daily routine checklist, recent sleep details, and a three-year sleep history heatmap based only on logged nights.
- A configurable Android wake-window notification, restored after reboot, plus a wind-down guide and on-screen timer.
- AI Coach and sleep-history integration already present in the project.

Behavior and daytime ratings are self-reported and saved locally. No wearable sleep-stage detection, clinical recovery measurement, or sensor-derived energy timeline is implemented. The wake alarm uses an Android notification window; Android power modes may delay it. These features have not been runtime-verified here.

## Before this ships

1. Configure and deploy the backend with `backend/.env` (see
   `backend/.env.example`) — Postgres URL, JWT secrets, and `ANTHROPIC_API_KEY`.
   Generate and apply a Prisma migration for `SleepSession.awakeMinutes`
   before syncing new sleep logs. No migration has been applied to a live DB.
2. Point the Android client's `API_BASE_URL` at your deployed backend with
   `-PnoctaApiBaseUrl=https://your-host/` or `NOCTA_API_BASE_URL`. Debug builds
   default to the Android emulator's local backend address (`10.0.2.2:4000`);
   this is not a deployed service. Release builds use an HTTPS URL from that
   same property; without one, they retain an example placeholder and cannot
   reach a backend.
3. The backend protects sleep and AI routes with JWT auth, but the Android
   client does not yet implement login/token storage/refresh. Live sync and AI
   calls therefore need that client auth flow before they can work end to end.
4. Run Android and backend checks on a configured machine; review alarm delivery
   and Room/Prisma migrations on a device and database.
5. Health Connect integration is only a dependency — no sensor data is read or displayed.
6. Add a real font license (see comment in `ui/theme/Type.kt`) before shipping.
7. Add the app icon, splash illustration, and any App Store / Play Store assets —
   none are included here since they're a design, not a code, deliverable.
