# Screen Checklist

Status legend: ✅ implemented locally · ◐ partial · ☐ not implemented. A local
screen or data path does not prove a configured backend or device integration.

| # | Screen              | Status | Notes |
|---|---------------------|--------|-------|
| 1 | Splash              | ☐ | |
| 2 | Welcome             | ☐ | |
| 3 | Onboarding          | ☐ | |
| 4 | Sleep Profile        | ◐ | Age-based duration targets are editable in Profile |
| 5 | Home                | ✅ | Score (or explicit no-data card), last night, recommendation, debt, quick actions, Vela character stage |
| 6 | Sleep Details       | ✅ | `sleep_details/{id}`: report from Room — factors, baseline delta, check-in, missing-data notes |
| 7 | Log Sleep           | ✅ | Manual entry with validation; saves as `SELF_REPORTED` |
| 8 | Sleep History       | ✅ | Week/month/year quality calendar under Tracker, backed by Room `sleep_sessions`; day tap opens full report |
| 9 | Sleep Optimization  | ✅ | Optimize tab: `PlanBedtime` from real history + editable wind-down routine |
| 10 | Wind-Down Mode      | ✅ | Deep-link to Optimize's routine checklist |
| 11 | AI Coach (landing)  | ✅ | Borderless Vela cutout header and empty state; 5 starter actions; context computed from stored records |
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

## UI composition rules (2.1 polish pass)

### Bottom navigation
- Six destinations: Home, Sleep, Optimize, Coach, Insights, Profile.
- The bar is a weighted `Row` (1/6 of the width per item) rather than
  Material's `NavigationBar`, whose fixed item width let "Optimize" wrap onto
  two lines. Labels are 11 sp, `maxLines = 1`, `softWrap = false`, so at the
  narrowest supported width (320 dp → 53 dp per slot) each label still renders
  whole: the widest is "Optimize" at roughly 46 dp.
- `BottomNavigationLayoutTest` (Robolectric + Compose) lays the bar out at
  320 dp and 360 dp and fails if any label is taller than one line or wider
  than its slot, and checks that tapping a label navigates.

### Vela (the character companion)
- `drawable-nodpi/vela_companion.webp` (full body) and `vela_portrait.webp`
  (head and shoulders) are genuinely transparent WebP files with an alpha
  channel — not a JPEG on a dark plate.
- Regenerate them from the artwork in `tools/source-art/` with
  `tools/cutout_vela.py`, which runs an offline segmentation model, cleans the
  matte, adds a faint violet silhouette aura and feathers the bottom edge.
  The four original JPEGs are no longer packaged in the APK.
- Rendering: `ContentScale.Fit`, sized from the artwork's aspect ratio, no
  clip, no border, no container. Home pins the full body to the bottom-trailing
  edge above a 300 dp stage (list content never sits underneath it at rest);
  dragging is clamped to the screen; tapping opens the Coach.
- The Coach header uses the portrait and the empty state uses the full body, so
  both surfaces frame the same character consistently.

### Empty and insufficient-data states
- A score exists only when at least one stored night passes validation
  (`SleepScoreBreakdown.hasData`, `trackedNights`). With no usable history the
  dashboard and Tracker show a "No score yet" card with a *Log a night* action
  instead of "0 — Needs attention".
- Records whose wake time is not after their sleep time (`hasUsableWindow`) are
  excluded from the dashboard, score, trends, calendar and night reports, and
  `SleepRepository.logSession` refuses to store one.
