import { Router } from "express";
import bcrypt from "bcryptjs";
import jwt from "jsonwebtoken";
import { z } from "zod";
import { prisma } from "../lib/prisma";

const router = Router();

const registerSchema = z.object({
  email: z.string().email(),
  password: z.string().min(8),
  name: z.string().min(1),
  timezone: z.string().default("UTC")
});

router.post("/register", async (req, res) => {
  const parsed = registerSchema.safeParse(req.body);
  if (!parsed.success) {
    return res.status(400).json({ error: parsed.error.flatten() });
  }
  const { email, password, name, timezone } = parsed.data;

  const existing = await prisma.user.findUnique({ where: { email } });
  if (existing) {
    return res.status(409).json({ error: "Email already registered" });
  }

  const passwordHash = await bcrypt.hash(password, 12);
  const user = await prisma.user.create({
    data: { email, passwordHash, name, timezone }
  });

  const tokens = issueTokens(user.id);
  res.status(201).json({ user: { id: user.id, email, name }, ...tokens });
});

const loginSchema = z.object({
  email: z.string().email(),
  password: z.string()
});

router.post("/login", async (req, res) => {
  const parsed = loginSchema.safeParse(req.body);
  if (!parsed.success) {
    return res.status(400).json({ error: parsed.error.flatten() });
  }
  const { email, password } = parsed.data;

  const user = await prisma.user.findUnique({ where: { email } });
  if (!user || !(await bcrypt.compare(password, user.passwordHash))) {
    // Same error for "no such user" and "wrong password" — don't leak which one.
    return res.status(401).json({ error: "Invalid email or password" });
  }

  const tokens = issueTokens(user.id);
  res.json({ user: { id: user.id, email: user.email, name: user.name }, ...tokens });
});

const refreshSchema = z.object({ refreshToken: z.string() });

router.post("/refresh", async (req, res) => {
  const parsed = refreshSchema.safeParse(req.body);
  if (!parsed.success) return res.status(400).json({ error: "refreshToken required" });

  try {
    const payload = jwt.verify(
      parsed.data.refreshToken,
      process.env.JWT_REFRESH_SECRET as string
    ) as { sub: string };
    res.json(issueTokens(payload.sub));
  } catch {
    res.status(401).json({ error: "Invalid refresh token" });
  }
});

function issueTokens(userId: string) {
  const accessToken = jwt.sign({ sub: userId }, process.env.JWT_SECRET as string, {
    expiresIn: "15m"
  });
  const refreshToken = jwt.sign({ sub: userId }, process.env.JWT_REFRESH_SECRET as string, {
    expiresIn: "30d"
  });
  return { accessToken, refreshToken };
}

export default router;
