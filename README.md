# Visitas

`Visitas` e um app Android em Kotlin com Jetpack Compose para criar cartoes pessoais personalizados, salvar esses cartoes localmente e compartilhar (PNG/PDF/vCard) com facilidade.

Na implementação mais recente foi, implemtado o Live update compátivel com a NOW BAR da samsung.

## Versao

- `1.10.0`

## O que o app faz

- Cria cartoes estilo cartao de visita com:
  nome, cargo, celular, email, Instagram, LinkedIn, URL, nota, foto e cor do passe.
- Mostra uma previa visual do passe antes de salvar.
- Salva os cartoes localmente no app com DataStore.
- Permite editar e excluir cartoes salvos.
- Compartilha cartoes no formato proprio `.visitas-card`, preservando campos e foto para outro celular com o app instalado.
- Inclui “Envio toque” com animacao antes de abrir o compartilhamento proximo do Android.
- Usa o Photo Picker do Android para escolher fotos, incluindo Google Fotos quando disponivel no aparelho.
- Tem navegacao inferior em pilula com Home, Salvos e Configuracoes (Criar fica no menu de 3 pontos).
- Mostra os 10 cartoes mais recentes na Home e todos os cartoes na aba Salvos.
- Home e Salvos tem menu de 3 pontos com Criar, Salvar ao Google Wallet, Partilhar, Editar e Excluir (Modo apresentação na Home).
- Permite trocar o idioma do app para Portugues (Brasil, Portugal e Angola), Ingles e Chines.
- Permite escolher um cartão no menu de 3 pontos e salvar no Google Wallet.
- Converte o cartão para `GenericClass`/`GenericObject`, cria/atualiza esse passe no backend e depois chama `savePassesJwt`.
- Mostra as informações dos cartões na Now bar ou no Live Update da samsung.
- Adptável a todos os formatos de tela incluíndo (Dobráveis e tablets).

## Arquitetura do Google Wallet **Brevemente**

O app segue o caminho oficial do Google Wallet:

- o app Android monta os dados do passe
- um backend seu assina o JWT
- o app chama `savePassesJwt` para abrir o Google Wallet

Isso significa que a assinatura do JWT nao fica dentro do APK. Esse backend e necessario para uso real.

Referencia oficial:

- [Issuing passes with the Android SDK](https://developers.google.com/wallet/generic/android)

## Estrutura principal

- `app/src/main/java/com/monst/transfiranow/ui`
  Interface, editor de cartoes e view model.
- `app/src/main/java/com/monst/transfiranow/data`
  Modelos e persistencia local.
- `app/src/main/java/com/monst/transfiranow/wallet`
  Montagem do payload do Google Wallet e cliente do backend JWT.
- `backend`
  Backend que cria/atualiza passes na Google Wallet API e assina o JWT.

## Requisitos

- Android Studio
- JDK 17+
- Android SDK 35+
- Google Wallet disponivel no dispositivo para testes do fluxo nativo
- Backend proprio para assinar o JWT do passe

## Como abrir

```bash
./gradlew assembleDebug
```

No Windows:

```powershell
.\gradlew.bat assembleDebug
```

## APK

O APK atual gerado para distribuicao manual fica em:

- `release/Visitas-v1.10.0-release.apk`

Para builds de release no GitHub Actions com backend publico, configure a variavel do repositório `CARDS_API_BASE_URL`.

### Backend no Vercel

O backend do Google Wallet pode ser publicado no Vercel como funcao serverless. A raiz do
repositorio ja contem `vercel.json`, que direciona as rotas para `backend/api/index.js`.
Antes de publicar, configure as variaveis de ambiente do projeto Vercel conforme
`backend/.env.example`. Nao use o Vercel Blob para o endpoint JWT: Blob serve para arquivos,
enquanto `/wallet/save-url` precisa rodar como API/serverless.

## Proximos passos sugeridos

- Criar uma build release assinada para distribuicao publica.

## Direitos autorais

© 2026 Erivaldo João. Todos os direitos reservados.
