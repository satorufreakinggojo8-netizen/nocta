import "dotenv/config";
import cors from "cors";
import express from "express";
import authRoutes from "./routes/auth";
import sleepRoutes from "./routes/sleep";
import aiRoutes from "./routes/ai";

const app = express();
app.use(cors());
app.use(express.json());

app.get("/health", (_req, res) => res.json({ status: "ok" }));

app.use("/v1/auth", authRoutes);
app.use("/v1/sleep", sleepRoutes);
app.use("/v1/ai", aiRoutes);

const port = process.env.PORT ? Number(process.env.PORT) : 4000;
app.listen(port, () => {
  console.log(`Nocta backend listening on :${port}`);
});
