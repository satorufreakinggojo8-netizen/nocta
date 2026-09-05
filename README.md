# Nocta — AI Sleep Optimization App

A premium, dark-first sleep-optimization app scaffold: Android client (Kotlin
+ Jetpack Compose) and a Node/TypeScript backend that proxies an AI sleep
coach ("Vela") grounded in the user's own logged data. Fully original
branding — no relation to any existing product.

## Repo layout

```
docs/           Architecture, database schema, and screen-status docs — read first
app/            Android app (Kotlin, Jetpack Compose, Clean Architecture + MVVM)
backend/        Node/TypeScript + Express + Prisma/PostgreSQL API + AI proxy
```

Start with `docs/01-ARCHITECTURE.md`, then `docs/02-DATABASE-SCHEMA.md`, then
`docs/03-SCREENS.md` for exactly what's implemented vs. stubbed.

## What's real vs. scaffolded

This is sized to hand to a mobile + backend team, not a finished Play Store
submission. Concretely implemented and working, not mocked:

- The full Home screen and AI Coach conversation screen, wired end to end
  (ViewModel → Repository → Room/Retrofit → real backend calls)
- The sleep score algorithm, as a pure unit-tested function
- Offline-first sleep logging (writes to Room immediately, syncs in the background)
- Token-by-token AI response streaming, client and server side
- Auth (register/login/refresh with JWT), sleep session CRUD, and the AI
  proxy endpoint on the backend, all against a real Postgres schema

Stubbed with real routes/ViewModels but placeholder UI (see `docs/03-SCREENS.md`
for the full list and why): Onboarding, Insights charts, Wind-Down Mode,
Settings, and a few others. They render an explicit "coming soon" state
rather than fabricated data.

## Before this ships

1. Fill in `backend/.env` (see `backend/.env.example`) — Postgres URL, JWT
   secrets, and an `ANTHROPIC_API_KEY`.
2. Point the Android client's `API_BASE_URL` (in `app/build.gradle.kts`) at
   your deployed backend.
3. Build out the remaining 🧩 screens following the pattern in
   `ui/screens/home/` and `ui/screens/coach/` (ViewModel + Repository +
   Composable, same as those two).
4. Health Connect integration is stubbed as a dependency only — no fake
   sensor data is read or displayed anywhere in this scaffold.
5. Add a real font license (see comment in `ui/theme/Type.kt`) before shipping.
6. Add the app icon, splash illustration, and any App Store / Play Store assets —
   none are included here since they're a design, not a code, deliverable.
