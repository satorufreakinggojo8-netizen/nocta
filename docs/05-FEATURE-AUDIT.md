# Nocta 3.0 — Feature Audit

Snapshot taken before this pass, on `feature/nocta-2-sleep-optimization`.

- HEAD at audit time: `7e927fe` (on top of `78c5c55`), working tree clean apart
  from an untracked `artifacts/` folder.
- Reported state confirmed by running the build: 91/91 unit tests passed,
  Kotlin compiled, `assembleDebug` produced an APK.
- No pull request exists for this branch yet; nothing has been pushed by this
  pass either.

Legend: ✅ implemented and unit-tested · ◐ partial / works but thin · ☐ not
implemented · ⛔ blocked in this environment (no device/emulator).

## 1. Sleep intelligence

| Capability | State | Evidence / note |
|---|---|---|
| Daily summary (bedtime, wake, duration, source) | ✅ | Home's single "Last night" line → `sleep_details/{id}` with source label from `DataSource` |
| Nightly report with factor explanations | ✅ | `BuildNightlyReport` + `SleepDetailScreen`; 9 tests |
| History with selectable nights | ✅ | `SleepCalendarCard` → `sleep_details/{id}` |
| Calendar distinguishes missing nights | ✅ | `SleepDayQuality.NO_RECORD` rendered as an outlined empty cell |
| 7/30/90-day trends | ✅ | `CalculateSleepTrends` + Insights segments; 9 tests |
| Bedtime/wake consistency | ✅ | Circular statistics (`CircularClockStats`) — midnight-safe |
| Duration trend + qualified sleep debt | ✅ | Chart in Insights (age-range target line, gaps, tappable nights); debt summary → `sleep_debt` page, always worded as an estimate |
| Milestones / achievable goals | ◐ | Streaks (current/longest) exist; no goal-setting flow |
| Score calculation explained | ✅ | Five factors with weights surfaced on all score cards |
| Missing/invalid record handling | ✅ | `hasUsableWindow` filter on every read path + write guard |
| Accessible chart labels and empty states | ✅ | Chart has a spoken night-by-night summary and a custom accessibility action per night; range selector has click labels; empty state offers *Log a night* |

Sleep stages: not claimed anywhere. Phone-only start/stop tracking is labelled
`Estimated · start/stop tracking`. See `docs/04-TRACKING-ALARMS-AUDIT.md`.

## 2. Bedtime planner

| Capability | State | Note |
|---|---|---|
| Personalized bedtime from wake time | ✅ | `PlanBedtime`, age-range target + 20 min onset allowance |
| Adjustable target wake time | ✅ | Profile + Optimize ±30 min, persisted in prefs |
| Wind-down duration | ✅ | New: 15–90 min in 5-min steps, feeds the plan and the routine |
| Explanation of the recommendation | ✅ | Each plan line is generated from real inputs |
| Safe schedule adjustment | ✅ | Max 20 min shift per night, anchored to actual recent average |
| Presets (school / weekend / custom) | ✅ | New: per-preset wake time, preview before applying |
| Preview before saving | ✅ | New: preview card with Apply / Cancel |
| Persistence across restarts | ✅ | SharedPreferences flows |
| Tests (midnight, DST, bounds) | ✅ | `PlanBedtimeTest` (11) + `PlannerLimitsTest` (6) |

## 3. Wind-down routines

| Capability | State | Note |
|---|---|---|
| Custom checklist | ◐ | Steps toggle on/off and persist; adding/removing/reordering steps is not implemented |
| Start / pause / complete / dismiss | ✅ | New `WindDownRun` state machine, durable in Room |
| Configurable duration | ✅ | Uses the planner's wind-down length |
| Breathing / relaxation timer | ◐ | "Five minutes of slow breathing" is a checklist item; no guided timer |
| Ambient sound | ☐ | No licensed assets in the project — deliberately not added |
| Progress history | ✅ | New `wind_down_runs`; real completed runs only |
| Tests | ✅ | `WindDownRunTest` (8) + Room round-trip tests |

## 4. Vela (AI coach)

| Capability | State | Note |
|---|---|---|
| Seven starter actions | ✅ | Analyze night / plan tonight / explain score / review week / journal patterns / one improvement / what's missing |
| Grounded in real data | ✅ | `BuildCoachContext` from sessions, profile, score, journal, routine runs |
| States insufficient data | ✅ | Explicit `[Missing Data: …]` block; empty state lists gaps |
| No invented measurements | ✅ | Context contains only computed facts; system prompt forbids invention |
| Chat history | ✅ | Room `ai_messages`, real timestamps on restore |
| Loading / error / retry / empty | ✅ | Typing dots, `isError` bubbles with Retry, empty state |
| Offline / backend unavailable | ✅ | `CoachFailures` + `BackendConfig`; placeholder URL shows "not configured" |
| Minimize personal transfer | ✅ | Journal free-text is never sent; only derived counts |
| Credentials server-side | ✅ | No key in the app; backend reads `ANTHROPIC_API_KEY` |
| No diagnosis claims | ✅ | Backend system prompt + in-app disclaimer |
| Tests | ✅ | `BuildCoachContextTest` (6), `CoachFailuresTest` (5) |

⛔ The app's `API_BASE_URL` is still the placeholder
(`https://api.nocta.example.com/`) and the backend is not deployed here, so
Vela cannot answer in a normal install. The UI now says exactly that instead of
showing endless retries.

## 5. Sleep journal & habit tracking

| Capability | State | Note |
|---|---|---|
| Restfulness, stress, naps, caffeine timing, exercise timing, screens, routine, notes | ✅ | New `journal_entries` (Room v5), all optional |
| Optional and editable | ✅ | Every field can be cleared; tri-state toggles are Yes/No/unanswered |
| Persistence | ✅ | Room, keyed by sleep date |
| Review and delete | ✅ | Seven-day selector + per-entry delete |
| Correlations gated by sample size | ✅ | Requires 3 nights on each side, ≥15 min difference |
| Labelled as patterns, not causes | ✅ | UI text + `JournalPattern.description` |
| Empty states and tests | ✅ | `JournalInsightsTest` (6), `RoomPersistenceTest` (5) |

## 6. Sleep environment & daily plan

| Capability | State | Note |
|---|---|---|
| Suggested bedtime + wind-down start | ✅ | Optimize plan card |
| Caffeine/exercise timing capture | ✅ | Journal fields |
| Reminder schedule | ☐ | Blocked — see `docs/04` §4–5 |
| Relaxation suggestions | ◐ | Static checklist step, no guided session |
| Bedroom-environment checklist | ☐ | Not built; would be a new screen |
| Goals and completion status | ◐ | Routine completion is recorded; no goal objects |

This group is the least complete part of the plan and is intentionally left
partial rather than shipped as decorative controls.

## 7. Health Connect & alarms

☐ **Not implemented, by decision.** `androidx.health.connect:connect-client`
is on the classpath but no permission is declared and no code uses it. Import
mapping (time zone, overlapping sessions, dedupe, permission UX) cannot be
validated without a device or the Health Connect app. Reminders/alarms need
`POST_NOTIFICATIONS`, exact-alarm policy handling, reboot re-scheduling and
snooze/dismiss verification — all untestable here. Constraints and the
implementation plan are recorded in `docs/04-TRACKING-ALARMS-AUDIT.md`.

## 8. Privacy & data management

| Capability | State | Note |
|---|---|---|
| Export of real records | ✅ | Nights + check-ins + journal + routine history + settings, JSON |
| Export format documented | ✅ | `docs/04` §6 + in-app copy; shared via system sheet, never uploaded |
| Safe deletion of everything personal | ✅ | `DeleteAllSleepData` covers sleep, tracking, check-ins, journal, runs; coach history separately |
| Confirmation before destructive actions | ✅ | Two dialogs, wording lists what is removed |
| Local vs remote deletion explained | ✅ | Copy states no server deletion endpoint exists |
| Migration safety | ◐ | v4→v5 DDL executed against real SQLite in tests; Room identity validation still needs an instrumented run |
| Minimal permissions | ✅ | `INTERNET` only |

## 9. UI, performance, reliability

- Nav order corrected to Home, Sleep, Coach, Optimize, Insights, Profile; the
  single-line guarantee is covered by `BottomNavigationLayoutTest`.
- Vela assets retained as genuinely transparent WebP; nothing was re-faked.
- New surfaces have loading/empty/error text; every new control writes to a
  repository (no decorative buttons).
- The companion is now a shared stage: a short tip bubble per screen above
  Vela's head, advancing per screen change, with a persisted mute
  (`SettingsPersistenceTest`) and an in-app fallback when no coach backend
  exists. The bubble is a labelled button with a polite live region and a
  separate 48 dp mute control.
- `AppShellTest` launches `MainActivity` under Robolectric with the real Hilt
  graph and a fresh Room database: it asserts the dashboard tip clears both
  primary actions, tab changes swap the tip without repeating it, muting hides
  the bubble but not the character, Profile's switch restores it, and Insights
  opens on its empty state. This is JVM-level automation, not a device run.
- Long explanations moved to five detail pages, so main tabs keep one line of
  context each. Charts have spoken summaries and per-night accessibility
  actions; no second chart repeats the same series.
- ◐ Responsive behaviour beyond the nav bar and the bubble geometry test
  (`ShadeStageTest`) is unverified — there is no emulator in this environment.
- ◐ Accessibility remains partial beyond the above: drag gestures have no
  non-gesture alternative, and TalkBack order has not been checked on a device.

## Next best step

Run the app on a phone or emulator and walk the new surfaces: the tip bubble on
each tab (tap with and without a backend), mute then re-enable from Profile, the
five detail pages, and tapping a bar on the Insights chart. The code paths and
the bubble's geometry are unit/Robolectric-tested, but no pixel has been
rendered on a device.
