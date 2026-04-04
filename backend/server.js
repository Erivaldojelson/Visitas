import "dotenv/config";
import express from "express";
import fs from "node:fs/promises";
import path from "node:path";
import crypto from "node:crypto";
import jwt from "jsonwebtoken";
import QRCode from "qrcode";

const app = express();
app.use(express.json({ limit: "1mb" }));

const port = Number(process.env.PORT || 8080);
const serviceAccountEmail = process.env.GOOGLE_SERVICE_ACCOUNT_EMAIL || "";
const privateKey = (process.env.GOOGLE_SERVICE_ACCOUNT_PRIVATE_KEY || "").replace(/\\n/g, "\n");
const origins = (process.env.GOOGLE_WALLET_ORIGINS || "")
  .split(",")
  .map((item) => item.trim())
  .filter(Boolean);

const cardsDataDir = process.env.CARDS_DATA_DIR || path.join(process.cwd(), "data");
const cardsFilePath = path.join(cardsDataDir, "cards.json");
const cardsPublicBaseUrl = (process.env.CARDS_PUBLIC_BASE_URL || `http://localhost:${port}`).replace(/\/+$/, "");

/** @type {Map<string, any>} */
const cardsById = new Map();

function validateConfig() {
  if (!serviceAccountEmail || !privateKey) {
    throw new Error("Configure GOOGLE_SERVICE_ACCOUNT_EMAIL e GOOGLE_SERVICE_ACCOUNT_PRIVATE_KEY.");
  }
}

function sanitizePayload(payload) {
  if (!payload || typeof payload !== "object") {
    throw new Error("Payload inválido.");
  }

  const claims = {
    iss: serviceAccountEmail,
    aud: "google",
    typ: "savetowallet",
    iat: Math.floor(Date.now() / 1000),
    origins,
    payload: {}
  };

  if (payload.payload && typeof payload.payload === "object") {
    claims.payload = payload.payload;
  } else {
    claims.payload = payload;
  }

  return claims;
}

function sanitizeCreateCardRequest(body) {
  if (!body || typeof body !== "object") {
    throw new Error("Body inválido.");
  }

  const name = typeof body.name === "string" ? body.name.trim() : "";
  const photo = typeof body.photo === "string" ? body.photo.trim() : "";

  if (name.length < 2) throw new Error("Informe um nome válido.");
  if (!photo) throw new Error("Informe uma URL de foto.");

  const instagram = typeof body.instagram === "string" ? body.instagram.trim() : "";
  const whatsapp = typeof body.whatsapp === "string" ? body.whatsapp.trim() : "";
  const website = typeof body.website === "string" ? body.website.trim() : "";

  return {
    name,
    photo,
    instagram: instagram || null,
    whatsapp: whatsapp || null,
    website: website || null
  };
}

async function ensureCardsDataDir() {
  await fs.mkdir(cardsDataDir, { recursive: true });
}

async function loadCardsFromDisk() {
  await ensureCardsDataDir();
  try {
    const raw = await fs.readFile(cardsFilePath, "utf8");
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return;
    for (const item of parsed) {
      if (item && typeof item === "object" && typeof item.id === "string") {
        cardsById.set(item.id, item);
      }
    }
  } catch (error) {
    if (error && typeof error === "object" && "code" in error && error.code === "ENOENT") return;
    throw error;
  }
}

async function saveCardsToDisk() {
  await ensureCardsDataDir();
  const payload = JSON.stringify(Array.from(cardsById.values()), null, 2);
  await fs.writeFile(cardsFilePath, payload, "utf8");
}

function buildCardUrl(id) {
  return `${cardsPublicBaseUrl}/cards/${encodeURIComponent(id)}`;
}

async function buildQrCodeDataUrl(value) {
  return QRCode.toDataURL(value, {
    errorCorrectionLevel: "M",
    margin: 1,
    width: 512
  });
}

app.get("/health", (_req, res) => {
  res.json({
    ok: true,
    service: "visitas-backend",
    cards: cardsById.size
  });
});

app.post("/cards", async (req, res) => {
  try {
    const request = sanitizeCreateCardRequest(req.body);
    const id = crypto.randomUUID();
    const cardUrl = buildCardUrl(id);
    const qrCode = await buildQrCodeDataUrl(cardUrl);

    const card = {
      id,
      ...request,
      qrCode
    };

    cardsById.set(id, card);
    await saveCardsToDisk();

    res.status(201).json(card);
  } catch (error) {
    res.status(400).json({
      error: error instanceof Error ? error.message : "Falha ao criar cartão."
    });
  }
});

app.get("/cards/:id", (req, res) => {
  const id = String(req.params.id || "");
  const card = cardsById.get(id);
  if (!card) {
    res.status(404).json({ error: "Cartão não encontrado." });
    return;
  }
  res.json(card);
});

app.post("/wallet/sign", (req, res) => {
  try {
    validateConfig();
    const claims = sanitizePayload(req.body);
    const token = jwt.sign(claims, privateKey, {
      algorithm: "RS256"
    });

    res.json({ jwt: token });
  } catch (error) {
    res.status(400).json({
      error: error instanceof Error ? error.message : "Falha ao assinar JWT."
    });
  }
});

await loadCardsFromDisk();

app.listen(port, () => {
  console.log(`Visitas backend listening on port ${port}`);
});
