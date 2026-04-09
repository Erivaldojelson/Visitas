# Changelog

## Visitas 1.9.5 - 2026-04-09

- Configurações reorganizadas em seções (estilo “Tomato”), deixando os atalhos mais claros.
- Primeira abertura do app: tela de boas-vindas pedindo permissão de notificações (Android 13+) e configurando canais.
- Home e Salvos: menu de 3 pontos com Criar, Partilhar, Editar e Excluir (e Modo apresentação na Home).
- Partilhar/Editar/Excluir: telas para escolher qual cartão antes da ação.
- Navegação: “Criar” saiu da pílula inferior e foi para o menu.
- Configurações: novo menu “Aparência” (Claro/Escuro/Sistema, Cor dinâmica e Tema preto puro).
- Removido “Adicionar ao Wallet do sistema” (incluindo widget e telas premium).
- Logo/ícone atualizado para estilo 3D (fundo branco + emoji de pessoa).
- UI: pílula inferior mais longa e com animação de expansão (estilo “Tomato”), gesto de voltar com pré-visualização (Predictive Back) e menu de 3 pontos fixo (estilo Expressive).
- Home/Salvos: card de estado vazio centralizado (mais próximo do meio).
- Configurações: Home mais limpo (Notificações/Idioma/Segurança/Backup movidos para “About” → “Mais opções”) e título centralizado.
- Now Bar: seletor de cor com círculos + branco/preto e ajuste de tonalidade.

## Visitas 1.9.0-alpha.4 - 2026-04-08

- Now Bar/Live Update: ícone da pílula (notificação recolhida) trocado para uma pessoa.

## Visitas 1.9.0-alpha.3 - 2026-04-08

- Correção: Live Update/Now Bar volta a aparecer no Android 16+ (notificação promovida sem `colorized` e sem BigPicture no modo promovido).

## Visitas 1.9.0-alpha.2 - 2026-04-08

- UI Premium → Recentes: adiciona atalho de Modo Evento para o cartão mais recente.
- Modo Evento: estado ativo/inativo agora é persistido e o botão muda para “Encerrar” quando estiver ativo.

## Visitas 1.9.0-alpha - 2026-04-07

- Configurações → Notificações: fluxo guiado para ativar permissões e abrir as configurações do sistema quando o app estiver bloqueado.
- Live Updates (Android 16+): direciona para a opção “Live notifications” nas configurações do sistema, atualizando o status ao voltar para o app.
- Live Updates: botão de teste agora dispara um Live Update para ajudar a opção aparecer no sistema.
- Live Updates: seletor de cor da pílula da Now Bar (espectro completo).
- Live Updates: chip mais curto e atalho para reativar o canal “Modo Evento” quando estiver desativado.
- Modo Evento (Android 16+): serviço em primeiro plano com cronômetro e timeout (59 min) para aumentar a chance de aparecer na Now Bar.
- Home → Recentes: atalho de “Modo apresentação” para o cartão mais recente (sem botão dentro do cartão).
- Ao criar um cartão: inicia automaticamente a Now Bar com nome, cargo, celular e Instagram.
- Live Update/Now Bar: mostra nome, cargo, celular, Instagram e QR Code.

## Visitas 1.8.5 - 2026-04-07

- Configurações → Notificações: switch para ativar, botão para abrir configurações do sistema e testes rápidos.
- Suporte a Live Updates no Android 16+ (notificação fixada/promovida com chip), com atalho para permitir nas configurações.
- Build atualizado para Android SDK 36 (AGP/Gradle) para suportar as APIs de Live Update.

## Visitas 1.8.4 - 2026-04-07

- Ajuste de espaçamento no topo: botão de voltar e cartão não encostam mais na barra de status.
- Bloqueio do app com biometria (impressão digital) com opção em Configurações.
- Layout adaptativo para telas grandes (tablet/dobráveis) e telas externas menores (ex.: Z Flip).

## Visitas 1.8.3 - 2026-04-07

- Tela de detalhes sem título no topo e sem texto “Voltar” (apenas botão circular transparente).
- Cartão com efeito glass/blur suave seguindo a cor do passe, incluindo QR code e textos sempre em branco para melhor legibilidade.

## Visitas 1.8.2 - 2026-04-07

- Cartão abre em uma nova tela de detalhes (sem expansão dentro da lista).
- Transições com animações spring (efeito elástico suave, estilo iOS).
- Fundo cinza com pontos brilhantes (efeito fluido/orgânico) nas telas de detalhes e no “gerar o cartão”.

## Visitas 1.0.0 - 2026-04-01

- Recriacao do projeto como app de cartoes pessoais.
- Editor de cartoes com cor personalizada, nome, cargo, celular, email, redes sociais, URL e nota.
- Previa visual do cartao dentro do app.
- Salvamento local com DataStore.
- Lista de cartoes salvos com edicao e exclusao.
- Integracao com Google Wallet Android SDK.
- Geracao de payload para passe generico e envio para backend assinador de JWT.
- Backend minimo adicionado para assinar o JWT do Google Wallet.
- APK inicial publicado como `Visitas-1.0.0.apk`.
- Barra inferior em formato de pilula com Home, Criar, Salvos e Configuracoes.
- Home mostrando os 10 cartoes mais recentes e tela Salvos com todos os cartoes.
- Configuracoes com versao, conta GitHub e troca de idioma para Portugues, Ingles e Chines.
- Suporte a foto no passe e novo visual inspirado em Material You.
- Logo do app atualizado.
