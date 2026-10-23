# Screen Checklist

Status legend: ✅ implemented locally · ◐ partial · ☐ not implemented. A local
screen or data path does not prove a configured backend or device integration.

| # | Screen              | Status | Notes |
|---|---------------------|--------|-------|
| 1 | Splash              | ☐ | |
| 2 | Welcome             | ☐ | |
| 3 | Onboarding          | ☐ | |
| 4 | Sleep Profile        | ◐ | Age-based duration targets are editable in Profile |
| 5 | Home                | ✅ | Minimal: score (or explicit no-data card), one last-night line, today's focus, Log sleep / Wind down |
| 6 | Sleep Details       | ✅ | `sleep_details/{id}`: report from Room — factors, baseline delta, check-in, missing-data notes |
| 7 | Log Sleep           | ✅ | Manual entry with validation; saves as `SELF_REPORTED` |
| 8 | Sleep History       | ✅ | Week/month/year quality calendar under Tracker, backed by Room `sleep_sessions`; day tap opens full report |
| 9 | Sleep Optimization  | ✅ | Optimize tab: `PlanBedtime` from real history, schedule presets with preview, adjustable wind-down length |
| 10 | Wind-Down Mode      | ✅ | Optimize routine runs: start / pause / resume / finish / dismiss, timed, with real completed history |
| 11 | AI Coach (landing)  | ✅ | Borderless Vela cutout header and empty state; 7 starter actions; "what I can see" panel listing real data and gaps |
| 12 | AI Conversation     | ✅ | Full streaming chat implementation |
| 13 | Insights            | ✅ | 7/30/90-day trends, duration chart with age-range target line, tappable bars, gaps for untracked nights, streaks, score distribution, sleep-debt link (Room only) |
| 14 | Weekly Report       | ◐ | Via Vela `Review my week` starter action; no dedicated screen |
| 15 | Goals               | ☐ | |
| 16 | Notifications       | ☐ | Not implemented — see docs/04-TRACKING-ALARMS-AUDIT.md §5 |
| 17 | Profile             | ✅ | Age target, wake time, Vela-tip mute switch, card mode (schedule presets live in Optimize) |
| 18 | Settings            | ◐ | Integrated into Profile |
| 19 | Privacy             | ✅ | Summary in Profile; full data-flow explanation on `privacy`; JSON export (nights, check-ins, journal, routines, settings) + delete controls |
| 20 | Health Connect      | ☐ | Dependency only; audit + constraints in docs/04-TRACKING-ALARMS-AUDIT.md |
| 21 | Sleep Journal       | ✅ | Seven-day journal: restfulness, stress, naps, caffeine/exercise timing, screens, routine, private notes, gated patterns |
| 22 | Companion guidance  | ✅ | One short tip per screen in a bubble above Vela's head; advances per screen change, mute persisted; falls back to in-app guidance when no coach backend exists |
| 23 | Explanation pages   | ✅ | `recommendation`, `sleep_debt`, `score_help`, `plan_why`, `privacy` — long copy lives here, not on the tabs |

A local screen or data path does not prove a configured backend or device integration.

## UI composition rules (2.1 polish pass)

### Bottom navigation
- Six destinations, in bar order: Home, Sleep, Coach, Optimize, Insights,
  Profile.
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
  clip, no border, no container. The shared stage pins the body to the
  bottom-trailing edge — 250 dp tall on Home, 150 dp on the other screens with
  a tip — and list content is inset so it never sits underneath it at rest;
  dragging is clamped to the screen; tapping opens the Coach.
- The Coach header uses the portrait and the empty state uses the full body, so
  both surfaces frame the same character consistently.

### Journal and routine
- Journal answers are optional; a `null` field is "not answered" and is never
  rendered as zero. Blank entries are deleted instead of stored.
- Pattern hints need at least three nights on each side of a factor and a
  15-minute difference, and are always labelled "possible association, not
  cause". Journal free-text never leaves the device.
- Routine runs are real records: timer with pause accounting, ticked steps, and
  a history list built only from completed runs.

### Screen composition rules (3.0 design pass)

- **Home is a summary, not a dashboard of everything.** Score, one last-night
  line, one focus line, two actions. Sleep debt moved to Insights; the
  recommendation's long form moved to its own page; quick actions dropped from
  four to two plus two small links.
- **Each tab owns its features.** Insights owns trends and the sleep-debt
  entry; Optimize owns the plan and routine; Sleep owns tracking, check-in,
  journal and history; Profile owns settings and data controls.
- **Long explanations live on pushed pages**, reached by tapping the item:
  `recommendation`, `sleep_debt`, `score_help` (score weights), `plan_why`
  (plan reasoning) and `privacy`. Main tabs keep a single line of context.
- **Icons and labels stay one line at 320 dp.** The bottom bar is unchanged
  from 2.1 (weighted row, 11 sp labels) and still guarded by
  `BottomNavigationLayoutTest`.
- **The companion stage is shared.** `NavGraph` renders Vela plus the tip
  bubble above `NavHost`, so the bubble survives tab changes instead of being
  recreated per screen. Lists reserve `ShadeLayout.bottomInset` at the bottom
  (360 dp on Home with the full-size character, 266 dp elsewhere) so their
  last row rests above the bubble.
- **The bubble sits above the head, never over the face.** The companion is
  drawn at 250 dp on Home and 150 dp elsewhere; the bubble is anchored
  `characterHeight + 6 dp` from the bottom. `ShadeStageTest` asserts the
  vertical order geometrically, and `AppShellTest` launches the real
  activity/nav graph to assert the dashboard bubble clears both primary
  actions, tabs swap the tip, muting hides only the bubble, Profile re-enables
  it, and Insights opens on a truthful empty state.
- Screens where a tip would fight the content (the chat itself, the log form,
  and the score/privacy/plan explanation pages) have no tip, so no bubble and
  no stage.

### Companion guidance

- `ShadeGuide` is a plain data table: route → up to two short tips, each with a
  one-line message, an in-app detail fallback and a question for the coach.
  Only features that exist in the build are described.
- `ShadeTipRotator` advances per screen, so re-entering a screen does not show
  the same sentence twice in a row; a screen with one tip repeats it.
- Tapping the bubble asks Vela the screen's question (route `coach_ask/{prompt}`)
  when `BackendConfig.isConfigured`; without a backend it expands the app's own
  guidance instead of showing a dead end or pretending to think.
- The tip text is a polite live region; the card is a button with an
  "Ask Vela"/"Show guidance" click label; mute is a separate 48 dp control with
  the label "Mute Vela's tips".
- Muting writes to `SettingsRepository` (SharedPreferences); the character
  stays, only the bubble hides, and Profile's "Vela's guidance" switch turns it
  back on. `SettingsPersistenceTest` proves a new repository instance (a
  restart) still reads the muted value.

### Charts

- The Insights duration chart is the single sleep-trend graph: bars per night
  across the selected 7/30/90-day range, day slots left empty for untracked
  nights (a gap is never drawn as zero), and a dashed line at the midpoint of
  the age-based target range.
- Tapping a bar opens that night's report; the same per-night actions are
  exposed to screen readers as custom accessibility actions, and the whole
  chart has a spoken summary. `InsightsChartTest` seeds a real night, opens
  Insights in the running app, checks the target legend and taps that night's
  bar to reach its report.
- No second chart repeats the same information: score distribution stays a
  labelled list, and streaks stay two metric tiles.

### Empty and insufficient-data states
- A score exists only when at least one stored night passes validation
  (`SleepScoreBreakdown.hasData`, `trackedNights`). With no usable history the
  dashboard and Tracker show a "No score yet" card with a *Log a night* action
  instead of "0 — Needs attention".
- Records whose wake time is not after their sleep time (`hasUsableWindow`) are
  excluded from the dashboard, score, trends, calendar and night reports, and
  `SleepRepository.logSession` refuses to store one.
