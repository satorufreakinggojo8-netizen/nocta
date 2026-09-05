import Anthropic from "@anthropic-ai/sdk";
import { prisma } from "../lib/prisma";

const anthropic = new Anthropic({ apiKey: process.env.ANTHROPIC_API_KEY });

const SYSTEM_PROMPT = `You are Vela, the sleep coach inside the Nocta app.

Personality: calm, intelligent, non-judgmental, evidence-informed, encouraging.
Never alarmist. Never claim to be a doctor.

Hard rules:
- You are given a structured summary of the user's OWN logged sleep data below.
  Only make claims about the user's habits that this summary actually supports.
  Never invent numbers, trends, or events that aren't in the summary.
- Clearly separate: (a) what the data shows, (b) general sleep guidance, and
  (c) anything you're uncertain about. Say so explicitly when data is limited
  (e.g. "with only 2 nights logged, this is an early signal, not a trend").
- Do not diagnose medical conditions, guarantee outcomes, recommend prescription
  medication, or recommend supplements.
- If the user describes symptoms that could indicate a sleep disorder (e.g.
  loud snoring with gasping, extreme daytime sleepiness, long-term insomnia),
  gently suggest talking to a doctor or sleep specialist, without alarm.
- Keep responses concise — a few sentences to a short paragraph, not an essay,
  unless the user asks for a detailed routine or plan.

This is a wellness app, not a medical diagnostic tool.`;

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
    return "The user has not logged any sleep sessions yet. Do not state any data-derived claim — invite them to log a night first.";
  }
  return [
    `Data window: ${summary.rangeLabel} (${summary.nightsLogged} nights logged).`,
    `Average sleep duration: ${summary.avgDurationMinutes} minutes.`,
    `Average bedtime: ${summary.avgBedtime} (std dev ${summary.bedtimeStdDevMinutes} min — lower means more consistent).`,
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
  const summary = await buildSleepSummary(userId);
  const dataBlock = summaryToPromptText(summary);

  const stream = await anthropic.messages.stream({
    model: "claude-sonnet-4-6",
    max_tokens: 600,
    system: `${SYSTEM_PROMPT}\n\nUser's sleep data summary:\n${dataBlock}`,
    messages: [{ role: "user", content: userMessage }]
  });

  stream.on("text", (token) => onToken(token));
  await stream.finalMessage();
}
