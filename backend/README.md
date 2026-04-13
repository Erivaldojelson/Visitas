# Visitas Backend (Cards + Google Wallet)

Backend para o app `Visitas` com dois papéis:

- servir APIs simples de cartão (`/cards`)
- integrar com a Google Wallet API para criar/atualizar a `GenericClass` e o `GenericObject`, depois assinar o JWT usado pelo app Android

## Endpoints

- `GET /health`
- `POST /cards`
- `GET /cards/{id}`
- `POST /wallet/save-url`
- `POST /wallet/sign`

## Fluxo do Google Wallet

1. O app Android envia os dados do cartão para o backend.
2. O backend converte esse cartão para o formato `GenericClass`/`GenericObject`.
3. O backend garante que a classe exista na Google Wallet API e faz upsert do objeto.
4. O backend assina um JWT `savetowallet`.
5. O app chama `savePassesJwt(...)` e abre o Google Wallet.

## Variáveis de ambiente

Use o arquivo `.env.example` como base:

- `PORT`
- `CARDS_PUBLIC_BASE_URL`
- `CARDS_DATA_DIR`
- `GOOGLE_WALLET_ORIGINS`
- `GOOGLE_WALLET_ISSUER_ID`
- `GOOGLE_WALLET_CLASS_SUFFIX`
- `GOOGLE_WALLET_CLASS_ID` (opcional)
- `GOOGLE_WALLET_ISSUER_NAME`
- `GOOGLE_SERVICE_ACCOUNT_EMAIL`
- `GOOGLE_SERVICE_ACCOUNT_PRIVATE_KEY`

Exemplo:

```env
PORT=8080
CARDS_PUBLIC_BASE_URL=https://api.seudominio.com
CARDS_DATA_DIR=./data
GOOGLE_WALLET_ORIGINS=https://pay.google.com,https://wallet.google.com
GOOGLE_WALLET_ISSUER_ID=0000000000000000000
GOOGLE_WALLET_CLASS_SUFFIX=visitas_card
GOOGLE_WALLET_ISSUER_NAME=Visitas
GOOGLE_SERVICE_ACCOUNT_EMAIL=wallet-signer@seu-projeto.iam.gserviceaccount.com
GOOGLE_SERVICE_ACCOUNT_PRIVATE_KEY="-----BEGIN PRIVATE KEY-----\nMIIEv...\n-----END PRIVATE KEY-----\n"
```

## Como habilitar a Google Wallet API no Google Cloud

1. Acesse o [Google Cloud Console](https://console.cloud.google.com/) e crie ou selecione um projeto.
2. Abra `APIs e serviços` > `Biblioteca`.
3. Pesquise por `Google Wallet API`.
4. Clique em `Ativar`.
5. Vá em `IAM e administrador` > `Contas de serviço`.
6. Crie uma service account para o backend.
7. Abra a service account e crie uma chave JSON.
8. Copie `client_email` para `GOOGLE_SERVICE_ACCOUNT_EMAIL`.
9. Copie `private_key` para `GOOGLE_SERVICE_ACCOUNT_PRIVATE_KEY`, mantendo as quebras de linha escapadas com `\n`.

## Como habilitar o emissor no Google Wallet

1. Acesse o [Google Pay & Wallet Console](https://pay.google.com/business/console).
2. Crie ou selecione seu issuer do Google Wallet.
3. Copie o `Issuer ID` para `GOOGLE_WALLET_ISSUER_ID`.
4. Defina um identificador fixo para a classe, por exemplo `visitas_card`, em `GOOGLE_WALLET_CLASS_SUFFIX`.
5. Adicione a service account do Google Cloud como usuária do issuer com permissão para emitir passes.
6. Para produção, conclua o processo de revisão do issuer/classe no console do Google Wallet.

## Como preparar o Android para chamar o Google Wallet

1. Gere o SHA-1 da chave usada no APK:

```powershell
.\gradlew.bat signingReport
```

2. No Google Cloud Console, configure o app Android autorizado com:
- pacote `com.monst.transfiranow`
- SHA-1 da assinatura usada no APK de teste ou da assinatura publicada

3. Instale o Google Wallet no celular de teste.

## Instalação local

```bash
npm install
npm run dev
```

No Windows PowerShell:

```powershell
npm install
npm run dev
```

## Teste rápido

Com o servidor rodando:

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/wallet/save-url" `
  -ContentType "application/json" `
  -Body (@{
    issuerId = "0000000000000000000"
    classSuffix = "visitas_card"
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
    walletPhotoUrl = "https://example.com/photo.png"
  } | ConvertTo-Json)
```

Resposta esperada:

```json
{
  "url": "https://pay.google.com/gp/v/save/...",
  "jwt": "eyJ...",
  "classId": "0000000000000000000.visitas_card",
  "objectId": "0000000000000000000.card_uuid_do_cartao"
}
```

## Observações importantes

- A imagem do cartão no Google Wallet precisa ser uma URL pública HTTPS.
- O backend faz upsert do objeto a cada envio, então mudanças no cartão podem ser refletidas em novos salvamentos.
- Para produção, prefira expor o backend em HTTPS e apontar `CARDS_PUBLIC_BASE_URL` para o domínio real.
- Se a classe ainda estiver em `UNDER_REVIEW`, o fluxo costuma servir para desenvolvimento e testes, mas a publicação ampla depende da aprovação do Google.

## Referências oficiais

- [Google Wallet Android SDK](https://developers.google.com/wallet/generic/android)
- [Autenticação com service account](https://developers.google.com/wallet/generic/use-cases/auth)
- [GenericClass REST reference](https://developers.google.com/wallet/reference/rest/v1/genericclass)
- [GenericObject REST reference](https://developers.google.com/wallet/reference/rest/v1/genericobject)
