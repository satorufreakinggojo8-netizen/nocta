# Screen Checklist

Status legend: ✅ implemented locally · ◐ partial · ☐ not implemented. A local
screen or data path does not prove a configured backend or device integration.

| # | Screen              | Status | Notes |
|---|---------------------|--------|-------|
| 1 | Splash              | ☐ | |
| 2 | Welcome             | ☐ | |
| 3 | Onboarding          | ☐ | |
| 4 | Sleep Profile        | ◐ | Age-based duration targets are editable in Profile |
| 5 | Home                | ✅ | Full: score, last night, recommendation, debt, quick actions |
| 6 | Sleep Details       | ✅ | `sleep_details/{id}`: report from Room — factors, baseline delta, check-in, missing-data notes |
| 7 | Log Sleep           | ✅ | Manual entry with validation; saves as `SELF_REPORTED` |
| 8 | Sleep History       | ✅ | Week/month/year quality calendar under Tracker, backed by Room `sleep_sessions`; day tap opens full report |
| 9 | Sleep Optimization  | ✅ | Optimize tab: `PlanBedtime` from real history + editable wind-down routine |
| 10 | Wind-Down Mode      | ✅ | Deep-link to Optimize's routine checklist |
| 11 | AI Coach (landing)  | ✅ | 5 starter actions; context computed from stored records |
| 12 | AI Conversation     | ✅ | Full streaming chat implementation |
| 13 | Insights            | ✅ | 7/30/90-day trends, duration chart, streaks, score distribution (Room only) |
| 14 | Weekly Report       | ◐ | Via Vela `Review my week` starter action; no dedicated screen |
| 15 | Goals               | ☐ | |
| 16 | Notifications       | ☐ | Not implemented — see docs/04-TRACKING-ALARMS-AUDIT.md §5 |
| 17 | Profile             | ✅ | Age target, wake time, card mode |
| 18 | Settings            | ◐ | Integrated into Profile |
| 19 | Privacy             | ✅ | Data-flow explanation + JSON export + delete controls in Profile |
| 20 | Health Connect      | ☐ | Dependency only; audit + constraints in docs/04-TRACKING-ALARMS-AUDIT.md |

A local screen or data path does not prove a configured backend or device integration.
