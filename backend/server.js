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
const walletIssuerId = process.env.GOOGLE_WALLET_ISSUER_ID || "";
const walletClassSuffix = process.env.GOOGLE_WALLET_CLASS_SUFFIX || "";
const walletClassId =
  process.env.GOOGLE_WALLET_CLASS_ID ||
  (walletIssuerId && walletClassSuffix ? `${walletIssuerId}.${walletClassSuffix}` : "");

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

function validateWalletPassConfig() {
  validateConfig();
  const issuer = getWalletIssuerId();
  if (!issuer) {
    throw new Error("Configure GOOGLE_WALLET_ISSUER_ID (ou GOOGLE_WALLET_CLASS_ID).");
  }
  if (!walletClassId) {
    throw new Error("Configure GOOGLE_WALLET_CLASS_ID (ou GOOGLE_WALLET_ISSUER_ID + GOOGLE_WALLET_CLASS_SUFFIX).");
  }
}

function getWalletIssuerId() {
  if (walletIssuerId) return walletIssuerId;
  if (!walletClassId) return "";
  const [issuer] = walletClassId.split(".");
  return issuer || "";
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

function sanitizeWalletSaveUrlRequest(body) {
  if (!body || typeof body !== "object") {
    throw new Error("Body inválido.");
  }

  const cardId = typeof body.cardId === "string" ? body.cardId.trim() : "";
  const name = typeof body.name === "string" ? body.name.trim() : "";
  const role = typeof body.role === "string" ? body.role.trim() : "";
  const phone = typeof body.phone === "string" ? body.phone.trim() : "";
  const email = typeof body.email === "string" ? body.email.trim() : "";
  const instagram = typeof body.instagram === "string" ? body.instagram.trim() : "";
  const linkedin = typeof body.linkedin === "string" ? body.linkedin.trim() : "";
  const website = typeof body.website === "string" ? body.website.trim() : "";
  const note = typeof body.note === "string" ? body.note.trim() : "";
  const passColor = typeof body.passColor === "string" ? body.passColor.trim() : "";
  const qrValue = typeof body.qrValue === "string" ? body.qrValue.trim() : "";

  const photoUrlRaw = typeof body.photoUrl === "string" ? body.photoUrl.trim() : "";
  const walletPhotoUrlRaw = typeof body.walletPhotoUrl === "string" ? body.walletPhotoUrl.trim() : "";
  const photoUrl = photoUrlRaw || walletPhotoUrlRaw;

  if (name.length < 2) throw new Error("Informe um nome válido.");

  return {
    cardId,
    name,
    role,
    phone,
    email,
    instagram,
    linkedin,
    website,
    note,
    passColor: normalizeHexBackgroundColor(passColor),
    qrValue,
    photoUrl: photoUrl || null
  };
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

function normalizeHexBackgroundColor(value) {
  if (!value) return "#0F766E";
  const trimmed = value.trim();
  if (/^#[0-9a-fA-F]{6}$/.test(trimmed)) return trimmed;
  if (/^#[0-9a-fA-F]{8}$/.test(trimmed)) return `#${trimmed.slice(3)}`;
  return "#0F766E";
}

function localized(value) {
  return {
    defaultValue: {
      language: "pt-BR",
      value
    }
  };
}

function textModule(header, body) {
  if (!body) return null;
  const trimmed = String(body).trim();
  if (!trimmed) return null;
  return { header, body: trimmed };
}

function normalizeUrl(handleOrUrl, prefix) {
  if (!handleOrUrl) return "";
  const trimmed = String(handleOrUrl).trim();
  if (!trimmed) return "";
  if (trimmed.startsWith("http")) return trimmed;
  return prefix + trimmed.replace(/^@/, "");
}

function phoneUrl(value) {
  if (!value) return "";
  const trimmed = String(value).trim();
  return trimmed ? `tel:${trimmed}` : "";
}

function emailUrl(value) {
  if (!value) return "";
  const trimmed = String(value).trim();
  return trimmed ? `mailto:${trimmed}` : "";
}

function link(label, url) {
  if (!url) return null;
  const trimmed = String(url).trim();
  if (!trimmed) return null;
  return {
    description: localized(label),
    uri: trimmed
  };
}

function image(url) {
  if (!url) return null;
  const trimmed = String(url).trim();
  if (!trimmed || !trimmed.startsWith("http")) return null;
  return {
    sourceUri: { uri: trimmed }
  };
}

function barcode(value) {
  if (!value) return null;
  const trimmed = String(value).trim();
  if (!trimmed) return null;
  return {
    type: "QR_CODE",
    value: trimmed,
    alternateText: trimmed
  };
}

function buildGenericObject(request) {
  const issuer = getWalletIssuerId();
  const suffixSource = request.cardId ? request.cardId : crypto.randomUUID();
  const objectSuffix = `card_${suffixSource.replace(/-/g, "_")}`;
  const objectId = `${issuer}.${objectSuffix}`;

  const links = [
    link("Website", request.website),
    link("Instagram", normalizeUrl(request.instagram, "https://instagram.com/")),
    link("LinkedIn", normalizeUrl(request.linkedin, "https://linkedin.com/in/")),
    link("Phone", phoneUrl(request.phone)),
    link("Email", emailUrl(request.email))
  ].filter(Boolean);

  const genericObject = {
    id: objectId,
    classId: walletClassId,
    state: "ACTIVE",
    hexBackgroundColor: request.passColor,
    cardTitle: localized(request.name),
    subheader: localized(request.role || "Cartão pessoal"),
    header: localized(request.phone || request.email || "Contato"),
    textModulesData: [
      textModule("Telefone", request.phone),
      textModule("Email", request.email),
      textModule("Instagram", request.instagram),
      textModule("LinkedIn", request.linkedin),
      textModule("URL", request.website),
      textModule("Nota", request.note)
    ].filter(Boolean),
    linksModuleData: { uris: links }
  };

  const photo = image(request.photoUrl);
  if (photo) {
    genericObject.heroImage = photo;
    genericObject.logo = photo;
  }

  const code = barcode(request.qrValue);
  if (code) {
    genericObject.barcode = code;
  }

  return genericObject;
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

app.post("/wallet/save-url", (req, res) => {
  try {
    validateWalletPassConfig();
    const request = sanitizeWalletSaveUrlRequest(req.body);
    const genericObject = buildGenericObject(request);

    const claims = {
      iss: serviceAccountEmail,
      aud: "google",
      typ: "savetowallet",
      iat: Math.floor(Date.now() / 1000),
      origins,
      payload: {
        genericObjects: [genericObject]
      }
    };

    const token = jwt.sign(claims, privateKey, { algorithm: "RS256" });
    const url = `https://pay.google.com/gp/v/save/${token}`;

    res.json({ url, jwt: token });
  } catch (error) {
    res.status(400).json({
      error: error instanceof Error ? error.message : "Falha ao gerar o link do Google Wallet."
    });
  }
});

await loadCardsFromDisk();

app.listen(port, () => {
  console.log(`Visitas backend listening on port ${port}`);
});
