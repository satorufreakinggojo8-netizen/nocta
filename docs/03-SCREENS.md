# Screen Checklist

Status legend: ✅ implemented in this scaffold · 🧩 stubbed (route + empty
Composable + TODO, wired into nav) · 📝 documented only (pattern to follow)

| # | Screen              | Status | Notes |
|---|---------------------|--------|-------|
| 1 | Splash              | ✅ | Logo + auth check → routes to Welcome or Home |
| 2 | Welcome             | ✅ | Value prop, Get Started |
| 3 | Onboarding          | 🧩 | Multi-step scaffold present; follows Home's ViewModel pattern |
| 4 | Sleep Profile        | 🧩 | Score reveal screen; reuses `SleepScoreCard` |
| 5 | Home                | ✅ | Full: score, last night, recommendation, debt, quick actions |
| 6 | Sleep Details       | 🧩 | |
| 7 | Log Sleep           | 🧩 | Form fields defined in domain model already |
| 8 | Sleep History       | 🧩 | |
| 9 | Sleep Optimization  | 🧩 | Content categories modeled in `OptimizationTip` |
| 10 | Wind-Down Mode      | 🧩 | State machine outlined in `WindDownViewModel` |
| 11 | AI Coach (landing)  | ✅ | Suggested prompts grid |
| 12 | AI Conversation     | ✅ | Full streaming chat implementation |
| 13 | Insights            | 🧩 | Chart components ready (`SleepChart`), data wiring pending |
| 14 | Weekly Report       | 🧩 | |
| 15 | Goals               | 🧩 | |
| 16 | Notifications       | 🧩 | |
| 17 | Profile             | 🧩 | |
| 18 | Settings            | 🧩 | |
| 19 | Privacy             | 🧩 | |
| 20 | Health Connect      | 🧩 | Adapter interface defined, no fake data returned |

Every 🧩 screen has a real Compose route registered in `NoctaNavGraph.kt` and a
real (if minimal) ViewModel — nothing renders fabricated data. They render an
explicit "Coming soon" state rather than pretending to be finished, per the
no-fake-implementation requirement.
