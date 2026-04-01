# Visitas Wallet Backend

Backend minimo para assinar o JWT do Google Wallet usado pelo app `Visitas`.

## O que ele faz

- recebe um payload JSON do app Android
- sobrescreve os claims principais do JWT
- assina com a service account do Google Wallet
- devolve:

```json
{ "jwt": "TOKEN_ASSINADO" }
```

## Endpoint

- `POST /wallet/sign`

## Variaveis de ambiente

Use o arquivo `.env.example` como base:

- `PORT`
- `GOOGLE_WALLET_ORIGINS`
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
GOOGLE_WALLET_ORIGINS=http://localhost:3000
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

Com o servidor rodando, envie o payload de exemplo:

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/wallet/sign" `
  -ContentType "application/json" `
  -InFile ".\sample-payload.json"
```

Resposta esperada:

```json
{ "jwt": "TOKEN_ASSINADO" }
```

## Contrato esperado do app

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
