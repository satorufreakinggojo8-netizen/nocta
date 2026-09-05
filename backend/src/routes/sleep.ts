import { Router } from "express";
import { z } from "zod";
import { prisma } from "../lib/prisma";
import { AuthedRequest, requireAuth } from "../middleware/auth";

const router = Router();
router.use(requireAuth);

router.get("/sessions", async (req: AuthedRequest, res) => {
  const since = typeof req.query.since === "string" ? new Date(req.query.since) : new Date(0);
  const sessions = await prisma.sleepSession.findMany({
    where: { userId: req.userId!, date: { gte: since } },
    orderBy: { date: "desc" }
  });
  res.json(sessions);
});

const sessionSchema = z.object({
  id: z.string().optional(),
  date: z.string(),
  bedtime: z.string(),
  sleepAttemptTime: z.string(),
  estimatedSleepTime: z.string(),
  wakeTime: z.string(),
  nightAwakenings: z.number().int().min(0),
  sleepQuality: z.number().int().min(1).max(5),
  morningEnergy: z.number().int().min(1).max(5),
  source: z.enum(["SELF_REPORTED", "ESTIMATED", "HEALTH_CONNECT", "WEARABLE"])
});

router.post("/sessions", async (req: AuthedRequest, res) => {
  const parsed = sessionSchema.safeParse(req.body);
  if (!parsed.success) return res.status(400).json({ error: parsed.error.flatten() });
  const data = parsed.data;

  const payload = {
    date: new Date(data.date),
    bedtime: new Date(data.bedtime),
    sleepAttemptTime: new Date(data.sleepAttemptTime),
    estimatedSleepTime: new Date(data.estimatedSleepTime),
    wakeTime: new Date(data.wakeTime),
    nightAwakenings: data.nightAwakenings,
    sleepQuality: data.sleepQuality,
    morningEnergy: data.morningEnergy,
    source: data.source
  };

  const session = data.id
    ? await prisma.sleepSession.update({ where: { id: data.id }, data: payload })
    : await prisma.sleepSession.create({ data: { userId: req.userId!, ...payload } });

  res.status(201).json(session);
});

export default router;
