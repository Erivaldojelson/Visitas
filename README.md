# Visitas

`Visitas` e um app Android em Kotlin com Jetpack Compose para criar cartoes pessoais personalizados, salvar esses cartoes localmente e enviar um passe generico para o Google Wallet pelo caminho oficial.

## Versao

- `1.0.0`

## O que o app faz

- Cria cartoes estilo cartao de visita com:
  nome, cargo, celular, email, Instagram, LinkedIn, URL, nota, foto e cor do passe.
- Mostra uma previa visual do passe antes de salvar.
- Salva os cartoes localmente no app com DataStore.
- Permite editar e excluir cartoes salvos.
- Tem navegacao inferior em pilula com Home, Criar, Salvos e Configuracoes.
- Mostra os 10 cartoes mais recentes na Home e todos os cartoes na aba Salvos.
- Permite trocar o idioma do app para Portugues (Brasil, Portugal e Angola), Ingles e Chines.
- Integra com o Google Wallet Android SDK para iniciar o fluxo de salvar passe.
- Gera o payload do passe generico no app e envia esse payload para um backend que assina o JWT.

## Arquitetura do Google Wallet

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
  Backend minimo para assinar o JWT do Google Wallet.

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

- `release/Visitas-1.0.0.apk`

## Proximos passos sugeridos

- Adicionar exportacao de imagem ou PDF do cartao.
- Criar uma build release assinada para distribuicao publica.
