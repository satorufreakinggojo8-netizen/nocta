import { PrismaClient } from "@prisma/client";

// Singleton so we don't exhaust Postgres connections across hot reloads / requests.
export const prisma = new PrismaClient();
