import { Router } from "express";
import { z } from "zod";
import { prisma } from "../lib/prisma";
import { AuthedRequest, requireAuth } from "../middleware/auth";
import { streamCoachResponse } from "../services/aiCoach";

const router = Router();
router.use(requireAuth);

const askSchema = z.object({
  conversationId: z.string().optional(),
  message: z.string().min(1).max(2000)
});

router.post("/coach", async (req: AuthedRequest, res) => {
  const parsed = askSchema.safeParse(req.body);
  if (!parsed.success) return res.status(400).json({ error: parsed.error.flatten() });
  const { message } = parsed.data;
  const userId = req.userId!;

  let conversationId = parsed.data.conversationId;
  if (!conversationId) {
    const convo = await prisma.aiConversation.create({
      data: { userId, title: message.slice(0, 80) }
    });
    conversationId = convo.id;
  }

  await prisma.aiMessage.create({
    data: { conversationId, role: "USER", content: message }
  });

  res.setHeader("Content-Type", "text/event-stream");
  res.setHeader("Cache-Control", "no-cache");
  res.setHeader("Connection", "keep-alive");
  res.flushHeaders();

  let fullResponse = "";
  try {
    await streamCoachResponse(userId, message, (token) => {
      fullResponse += token;
      res.write(`data: ${token}\n\n`);
    });
  } catch (err) {
    console.error("AI coach stream error:", err);
    res.write(`data: I couldn't reach the coach right now. Please try again in a moment.\n\n`);
  } finally {
    res.write("data: [DONE]\n\n");
    res.end();
  }

  if (fullResponse) {
    await prisma.aiMessage.create({
      data: { conversationId, role: "ASSISTANT", content: fullResponse }
    });
  }
});

router.get("/coach/:conversationId", async (req: AuthedRequest, res) => {
  const conversation = await prisma.aiConversation.findFirst({
    where: { id: req.params.conversationId, userId: req.userId! },
    include: { messages: { orderBy: { createdAt: "asc" } } }
  });
  if (!conversation) return res.status(404).json({ error: "Not found" });
  res.json(conversation);
});

export default router;
