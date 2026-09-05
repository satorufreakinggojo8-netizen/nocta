# Nocta Backend

Node.js + TypeScript + Express + Prisma/PostgreSQL. Proxies AI coach requests
to Anthropic so the mobile client never holds an API key.

## Setup

```bash
cp .env.example .env   # fill in DATABASE_URL, JWT secrets, ANTHROPIC_API_KEY
npm install
npm run prisma:generate
npm run prisma:migrate   # creates tables from prisma/schema.prisma
npm run dev
```

## Endpoints implemented in this scaffold

- `POST /v1/auth/register`, `/login`, `/refresh` — JWT access (15m) + refresh (30d)
- `GET/POST /v1/sleep/sessions` — CRUD for sleep sessions (auth required)
- `POST /v1/ai/coach` — streams Vela's response as Server-Sent Events; persists
  both the user's message and the full assistant reply to `AiConversation`/`AiMessage`
- `GET /v1/ai/coach/:conversationId` — fetch a past conversation

## Not yet implemented (documented, not faked)

Insight computation jobs (Daily/WeeklyInsight population), notification
scheduling/delivery, and Health Connect data ingestion are modeled in the
schema but have no route/service yet — see `docs/03-SCREENS.md` in the repo
root for what depends on each.

## Where the AI grounding happens

`src/services/aiCoach.ts` pulls the last 30 days of `SleepSession` rows,
reduces them to a small set of numbers (average duration, bedtime, bedtime
variability, quality, awakenings, weekday/weekend gap), and puts only those
numbers in Claude's system prompt — the model is instructed not to state any
data claim beyond what's in that summary. This is the mechanism behind the
product requirement that the AI "must distinguish between observed data,
general guidance, and uncertain conclusions" and "never invent data."
