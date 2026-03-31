# Transfira-now

`Transfira-now` e um app Android em Kotlin com Jetpack Compose e Material 3 focado em acompanhar downloads em segundo plano e espelhar o progresso em uma notificacao continua preparada para surfaces como lock screen e Now Bar quando o sistema suportar esse fluxo.

## Versao

- `1.0.0`

## O que o app faz

- Monitora downloads detectados nas notificacoes de outros apps via `NotificationListenerService`.
- Mostra uma tela principal inspirada no visual do Nowbar Meter, com estilo Material You.
- Exibe itens de transferencia com icone de progresso mais expressivo, barra de progresso e status.
- Mantem notificacao continua em segundo plano.
- Mostra informacoes publicas de progresso na tela de bloqueio.
- Tenta usar o caminho de `ProgressStyle` em Android 16 para melhor integracao com superficies de progresso do sistema.
- Permite personalizar a cor principal do app/notificacao.

## Limitacoes importantes

- A Samsung nao oferece uma API publica confiavel para forcar um layout customizado dentro da Now Bar.
- O app segue o caminho suportado pelo Android com notificacao de progresso continua e categorizada, mas a renderizacao final na Now Bar depende da One UI.
- O monitoramento depende de notificacoes publicadas por outros apps. Se um app nao expuser progresso por notificacao, o `Transfira-now` nao consegue inferir o download com a mesma precisao.

## Tecnologias

- Kotlin
- Jetpack Compose
- Material 3 / Material You
- Android foreground service
- Notification listener
- Android Splash Screen API

## Estrutura principal

- `app/src/main/java/com/monst/transfiranow/ui`
  Tela principal e componentes Compose.
- `app/src/main/java/com/monst/transfiranow/service`
  Servicos de monitoramento, boot e notificacao.
- `app/src/main/java/com/monst/transfiranow/data`
  Modelos e repositorio em memoria.

## Requisitos

- Android Studio
- JDK 17+
- Android SDK 35+

## Como abrir

```bash
./gradlew assembleDebug
```

No Windows:

```powershell
.\gradlew.bat assembleDebug
```

## APK

O APK debug gerado fica em:

- `app/build/outputs/apk/debug/app-debug.apk`

Para distribuir para outras pessoas, o ideal e publicar esse arquivo em um Release do GitHub ou em outra hospedagem de arquivos.

## Permissoes relevantes

- `POST_NOTIFICATIONS`
- `FOREGROUND_SERVICE`
- `FOREGROUND_SERVICE_DATA_SYNC`
- `RECEIVE_BOOT_COMPLETED`
- `BIND_NOTIFICATION_LISTENER_SERVICE` no servico de listener

## Proximos passos sugeridos

- Persistir preferencias com DataStore.
- Adicionar historico de transferencias.
- Assinar uma build release para distribuicao publica.
