# Visitas Backend (Cards + Wallet)

Backend para o app `Visitas` com:

- API de cartões (`/cards`) que gera um QR Code (base64 PNG) e retorna o payload usado pelo app Android
- geração do link do Google Wallet (`/wallet/save-url`) no formato `https://pay.google.com/gp/v/save/{JWT}`
- (opcional) assinatura bruta do JWT (`/wallet/sign`) caso você queira enviar o payload pronto

## Endpoints

- `GET /health`
- `POST /cards`
- `GET /cards/{id}`
- `POST /wallet/save-url`
- `POST /wallet/sign`

## Contrato do app (Cards)

O app envia:

```json
{
  "name": "Seu Nome",
  "photo": "https://...",
  "instagram": "usuario",
  "whatsapp": "+55...",
  "website": "https://..."
}
```

E recebe:

```json
{
  "id": "uuid",
  "name": "Seu Nome",
  "photo": "https://...",
  "instagram": "usuario",
  "whatsapp": "+55...",
  "website": "https://...",
  "qrCode": "data:image/png;base64,..."
}
```

## Variáveis de ambiente

Use o arquivo `.env.example` como base:

- `PORT`
- `CARDS_PUBLIC_BASE_URL`
- `CARDS_DATA_DIR`
- `GOOGLE_WALLET_ORIGINS`
- `GOOGLE_WALLET_ISSUER_ID`
- `GOOGLE_WALLET_CLASS_SUFFIX` (ou `GOOGLE_WALLET_CLASS_ID`)
- `GOOGLE_SERVICE_ACCOUNT_EMAIL`
- `GOOGLE_SERVICE_ACCOUNT_PRIVATE_KEY`

## Como configurar

1. Crie o arquivo `backend/.env` a partir de `backend/.env.example`.
2. Preencha o email da service account do Google Cloud.
3. Cole a chave privada JSON da service account no campo `GOOGLE_SERVICE_ACCOUNT_PRIVATE_KEY`, mantendo os `\n`.
4. Ajuste `GOOGLE_WALLET_ORIGINS` para os dominios permitidos no seu fluxo.

Exemplo:

```env
PORT=8080
CARDS_PUBLIC_BASE_URL=http://localhost:8080
CARDS_DATA_DIR=./data
GOOGLE_WALLET_ORIGINS=http://localhost:3000
GOOGLE_WALLET_ISSUER_ID=0000000000000000000
GOOGLE_WALLET_CLASS_SUFFIX=visitas_card
GOOGLE_SERVICE_ACCOUNT_EMAIL=wallet-signer@seu-projeto.iam.gserviceaccount.com
GOOGLE_SERVICE_ACCOUNT_PRIVATE_KEY="-----BEGIN PRIVATE KEY-----\nMIIEv...\n-----END PRIVATE KEY-----\n"
```

## Exemplo de execucao

```bash
npm install
npm run dev
```

No Windows PowerShell:

```powershell
npm install
npm run dev
```

## Exemplo de teste local

### Criar cartão

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/cards" `
  -ContentType "application/json" `
  -Body (@{
    name = "Seu Nome"
    photo = "https://example.com/photo.png"
  } | ConvertTo-Json)
```

### Assinar JWT do Wallet

Com o servidor rodando, envie o payload de exemplo:

```json
{ "jwt": "TOKEN_ASSINADO" }
```

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/wallet/sign" `
  -ContentType "application/json" `
  -InFile ".\sample-payload.json"
```

O app pode enviar o payload inteiro ou um objeto no formato:

```json
{
  "payload": {
    "genericClasses": [],
    "genericObjects": []
  }
}
```

O backend completa:

- `iss`
- `aud`
- `typ`
- `iat`
- `origins`

e depois assina via `RS256`.

O arquivo `sample-payload.json` neste diretorio serve como ponto de partida para esse teste.

### Gerar link do Wallet (recomendado)

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/wallet/save-url" `
  -ContentType "application/json" `
  -Body (@{
    cardId = "uuid-do-cartao"
    name = "Seu Nome"
    role = "Seu cargo"
    phone = "+55 31 00000-0000"
    email = "email@exemplo.com"
    instagram = "@seuuser"
    linkedin = "seuuser"
    website = "https://example.com"
    note = "Nota opcional"
    passColor = "#1E3A8A"
    qrValue = "https://example.com"
    photoUrl = "https://example.com/photo.png"
  } | ConvertTo-Json)
```

Resposta:

```json
{ "url": "https://pay.google.com/gp/v/save/..." }
```

## Deploy na Oracle Cloud (Always Free)

Objetivo: subir esse backend em uma VM Always Free (AMD ou ARM) e expor via IP público ou domínio.

1. Crie uma VM no Oracle Cloud Compute (Always Free) e habilite SSH.
2. Libere entrada (ingress) para a porta do backend (ex.: `8080`) na Security List/NSG do seu VCN.
3. Acesse via SSH e instale Docker + Compose.
4. Clone o repositório na VM e crie `backend/.env` a partir de `.env.example`.
5. Suba com Docker Compose:

```bash
cd backend
docker compose up -d --build
```

6. Teste:

```bash
curl http://localhost:8080/health
```

Dica: em produção, prefira colocar um reverse proxy (Nginx/Caddy) com TLS e apontar `CARDS_PUBLIC_BASE_URL` para o seu domínio (ex.: `https://api.seudominio.com`).
