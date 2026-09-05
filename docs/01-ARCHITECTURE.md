# Nocta — Product & Technical Architecture

Original branding used throughout: app name **Nocta**, AI coach named **Vela**.
Nothing here reuses Somnr's name, marks, or UI — this is an independent design.

## 1. Product Architecture

Nocta is a client/server system:

- **Android client** (Kotlin, Jetpack Compose) — all user-facing UI, local caching,
  offline-first logging, Health Connect integration.
- **Backend API** (Node.js/TypeScript, Express, PostgreSQL via Prisma) — auth,
  sleep data persistence, insight computation, and the AI coach proxy.
- **LLM provider** (Anthropic API) — called only from the backend, never from the
  client. The backend assembles a data-grounded prompt and validates the reply
  before it reaches the app.

Data flow for a typical AI question:

```
Compose UI → ViewModel → Repository → Retrofit → Backend /ai/coach
   Backend: load user + last 30 days of SleepSession/SleepLog
          → build structured summary (durations, consistency, timing, notes)
          → construct system prompt (persona + guardrails + summary)
          → call Claude → validate/shape response → stream back to client
   Client: renders streamed tokens in AIMessageBubble
```

## 2. Why this stack

Kotlin + Jetpack Compose is the right default for an Android-first, premium
health/wellness app: native performance for animations (score counting, chart
transitions, wind-down fades), first-class Health Connect support, and Compose's
declarative model maps cleanly onto the state-heavy screens here (Home, Insights,
AI Coach streaming). A cross-platform framework was considered and rejected —
Health Connect integration and fine animation control are meaningfully harder
to get right outside native Android, and this app has no near-term iOS requirement
stated.

Node/TypeScript + PostgreSQL for the backend: the workload is mostly CRUD +
aggregation + one external API proxy — nothing here needs a heavier framework,
and TypeScript gives the API layer the same strong typing the client has in
Kotlin. Prisma gives real migrations and relational integrity without hand-written
SQL for every query.

## 3. Layering (Clean Architecture + MVVM, Android side)

```
presentation/   Compose screens + ViewModels (UI state, one-way data flow)
domain/         Use cases, domain models, repository interfaces (no Android deps)
data/           Repository implementations, Room DAOs/entities, Retrofit services,
                Health Connect adapters, DataStore preferences
di/             Hilt modules wiring the above together
```

ViewModels expose a single `StateFlow<UiState>` per screen and a small set of
one-shot `UiEvent`s (snackbars, navigation) via `SharedFlow`. No business logic
lives in Composables.

## 4. Where secrets go

- The Android app never holds an Anthropic API key. It authenticates to the
  Nocta backend with a JWT (short-lived access + refresh token, stored in
  EncryptedSharedPreferences / DataStore with `Preferences.Key` encryption).
- The backend reads `ANTHROPIC_API_KEY`, `DATABASE_URL`, `JWT_SECRET`, and
  `JWT_REFRESH_SECRET` from environment variables (`.env`, not committed).
  See `backend/.env.example`.

## 5. Implementation stages (order followed in this scaffold)

1. Design system (color/type/shape/theme tokens) — done, see `app/.../ui/theme`.
2. Domain models + Room schema — done, see `app/.../domain/model`, `data/local`.
3. Navigation graph + bottom nav — done, see `app/.../navigation`.
4. Core reusable components (cards, bubbles, buttons) — done, see `ui/components`.
5. Home + AI Coach screens fully wired (representative of the full 20-screen
   list) — done. Remaining screens (Sleep Details, Insights charts, Wind-Down,
   Settings, etc.) follow the exact same ViewModel/Repository/Compose pattern
   and are stubbed with TODO markers rather than faked — see `docs/03-SCREENS.md`
   for the full checklist and which are implemented vs. scaffolded.
6. Backend: auth, sleep CRUD, AI proxy with streaming — done, minimal but real
   (not mocked) against Prisma/Postgres.
7. Tests: one unit test for the sleep score algorithm, one for the AI prompt
   builder (pure functions, the highest-value places for unit tests here).

This is a scaffold sized to hand to a mobile team, not a finished App-Store
submission — see `docs/06-NEXT-STEPS.md` for what's left before shipping.
