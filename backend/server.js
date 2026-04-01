import "dotenv/config";
import express from "express";
import jwt from "jsonwebtoken";

const app = express();
app.use(express.json({ limit: "1mb" }));

const port = Number(process.env.PORT || 8080);
const serviceAccountEmail = process.env.GOOGLE_SERVICE_ACCOUNT_EMAIL || "";
const privateKey = (process.env.GOOGLE_SERVICE_ACCOUNT_PRIVATE_KEY || "").replace(/\\n/g, "\n");
const origins = (process.env.GOOGLE_WALLET_ORIGINS || "")
  .split(",")
  .map((item) => item.trim())
  .filter(Boolean);

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

app.get("/health", (_req, res) => {
  res.json({ ok: true, service: "visitas-wallet-backend" });
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

app.listen(port, () => {
  console.log(`Visitas wallet backend listening on port ${port}`);
});
