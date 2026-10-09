import Anthropic from "@anthropic-ai/sdk";
import { prisma } from "../lib/prisma";

const anthropic = new Anthropic({ apiKey: process.env.ANTHROPIC_API_KEY });

const SYSTEM_PROMPT = `You are Vela, the Shadow AI Sleep Coach inside Nocta.

Personality & Tone:
- Sharp, focused, dark cinematic persona (inspired by elite shadow guides).
- Calm, direct, deeply observant, encouraging, and evidence-informed.
- Speak with quiet mastery: "Control your recovery. Level up your circadian discipline."
- Never alarmist. Never claim to be a medical physician or doctor.

Hard rules:
- You are provided with structured sleep context and logged user data below.
  Only make claims about the user's habits that this data actually supports.
  Never invent numbers, trends, or false events.
- Clearly separate:
  (a) what the logged data confirms,
  (b) evidence-backed sleep optimization advice, and
  (c) any uncertainties due to limited logs.
  Explicitly state when data is limited (e.g. "With only 1 night logged, this is a baseline reading, not a long-term pattern.").
- Do not diagnose medical conditions, guarantee medical outcomes, or prescribe drugs or supplements.
- If symptoms like severe gasping, sleep apnea, or extreme chronic fatigue arise, recommend consulting a medical professional calmly.
- Keep responses concise and impactful — typically 2 to 4 powerful sentences unless asked for an in-depth bedtime protocol.

This is a high-performance wellness and circadian optimization platform.`;

interface SleepDataSummary {
  rangeLabel: string;
  nightsLogged: number;
  avgDurationMinutes: number | null;
  avgBedtime: string | null;
  bedtimeStdDevMinutes: number | null;
  avgQuality: number | null;
  avgAwakenings: number | null;
  weekdayVsWeekendBedtimeGapMinutes: number | null;
}

/** Pulls the last 30 days of sessions and reduces them to the numbers Vela is allowed to cite. */
async function buildSleepSummary(userId: string): Promise<SleepDataSummary> {
  const since = new Date();
  since.setDate(since.getDate() - 30);

  const sessions = await prisma.sleepSession.findMany({
    where: { userId, date: { gte: since } },
    orderBy: { date: "asc" }
  });

  if (sessions.length === 0) {
    return {
      rangeLabel: "last 30 days",
      nightsLogged: 0,
      avgDurationMinutes: null,
      avgBedtime: null,
      bedtimeStdDevMinutes: null,
      avgQuality: null,
      avgAwakenings: null,
      weekdayVsWeekendBedtimeGapMinutes: null
    };
  }

  const durations = sessions.map(
    (s) => (s.wakeTime.getTime() - s.estimatedSleepTime.getTime()) / 60000
  );
  const bedtimeMinutes = sessions.map((s) => s.bedtime.getHours() * 60 + s.bedtime.getMinutes());
  const avgBedtimeMin = mean(bedtimeMinutes);

  const weekday = sessions.filter((s) => ![0, 6].includes(s.date.getDay()));
  const weekend = sessions.filter((s) => [0, 6].includes(s.date.getDay()));
  const weekdayAvg = weekday.length
    ? mean(weekday.map((s) => s.bedtime.getHours() * 60 + s.bedtime.getMinutes()))
    : null;
  const weekendAvg = weekend.length
    ? mean(weekend.map((s) => s.bedtime.getHours() * 60 + s.bedtime.getMinutes()))
    : null;

  return {
    rangeLabel: "last 30 days",
    nightsLogged: sessions.length,
    avgDurationMinutes: Math.round(mean(durations)),
    avgBedtime: minutesToClock(avgBedtimeMin),
    bedtimeStdDevMinutes: Math.round(stdDev(bedtimeMinutes)),
    avgQuality: Math.round(mean(sessions.map((s) => s.sleepQuality)) * 10) / 10,
    avgAwakenings: Math.round(mean(sessions.map((s) => s.nightAwakenings)) * 10) / 10,
    weekdayVsWeekendBedtimeGapMinutes:
      weekdayAvg !== null && weekendAvg !== null ? Math.round(Math.abs(weekdayAvg - weekendAvg)) : null
  };
}

function mean(values: number[]): number {
  return values.reduce((a, b) => a + b, 0) / values.length;
}

function stdDev(values: number[]): number {
  const m = mean(values);
  return Math.sqrt(mean(values.map((v) => (v - m) ** 2)));
}

function minutesToClock(totalMinutes: number): string {
  const h = Math.floor(totalMinutes / 60) % 24;
  const m = Math.round(totalMinutes % 60);
  const period = h >= 12 ? "PM" : "AM";
  const displayHour = h % 12 === 0 ? 12 : h % 12;
  return `${displayHour}:${m.toString().padStart(2, "0")} ${period}`;
}

function summaryToPromptText(summary: SleepDataSummary): string {
  if (summary.nightsLogged === 0) {
    return "The user has no recorded sleep logs in the database yet. Advise them to log a sleep session to begin data tracking.";
  }
  return [
    `Data window: ${summary.rangeLabel} (${summary.nightsLogged} nights logged).`,
    `Average sleep duration: ${summary.avgDurationMinutes} minutes.`,
    `Average bedtime: ${summary.avgBedtime} (std dev ${summary.bedtimeStdDevMinutes} min — lower means better circadian consistency).`,
    `Average self-reported sleep quality: ${summary.avgQuality}/5.`,
    `Average night awakenings: ${summary.avgAwakenings}.`,
    summary.weekdayVsWeekendBedtimeGapMinutes !== null
      ? `Weekday vs weekend bedtime gap: ~${summary.weekdayVsWeekendBedtimeGapMinutes} minutes.`
      : null
  ]
    .filter(Boolean)
    .join("\n");
}

/**
 * Streams Claude's response as Server-Sent-Events ("data: <token>\n\n" lines,
 * terminated by "data: [DONE]\n\n") so the Android client can render tokens
 * progressively. See AiCoachRepositoryImpl.kt on the client for the reader.
 */
export async function streamCoachResponse(
  userId: string,
  userMessage: string,
  onToken: (token: string) => void
): Promise<void> {
  let summary: SleepDataSummary;
  try {
    summary = await buildSleepSummary(userId);
  } catch (err) {
    summary = {
      rangeLabel: "last 30 days",
      nightsLogged: 0,
      avgDurationMinutes: null,
      avgBedtime: null,
      bedtimeStdDevMinutes: null,
      avgQuality: null,
      avgAwakenings: null,
      weekdayVsWeekendBedtimeGapMinutes: null
    };
  }
  const dataBlock = summaryToPromptText(summary);

  const stream = await anthropic.messages.stream({
    model: "claude-3-5-sonnet-20241022",
    max_tokens: 600,
    system: `${SYSTEM_PROMPT}\n\nUser's logged sleep summary:\n${dataBlock}`,
    messages: [{ role: "user", content: userMessage }]
  });

  stream.on("text", (token) => onToken(token));
  await stream.finalMessage();
}
