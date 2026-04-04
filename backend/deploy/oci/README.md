# Deploy na Oracle Cloud (Always Free)

Este guia sobe o `backend/` em uma VM Always Free (AMD ou ARM) usando Docker.

## 1) Criar VM

- Crie uma instância em **Compute** (Always Free).
- Gere/adicione sua chave SSH.
- No VCN/Security List (ou NSG), libere **ingress TCP** para:
  - `22` (SSH)
  - `8080` (API) — ou use `80/443` se for colocar reverse proxy

> Importante: além das regras de rede do OCI, verifique também firewall do SO (quando aplicável).

## 2) Acessar via SSH

```bash
ssh -i /caminho/para/sua-chave ubuntu@SEU_IP_PUBLICO
```

## 3) Instalar Docker (Ubuntu)

Dentro da VM:

```bash
sudo apt-get update
sudo apt-get install -y docker.io docker-compose-plugin git
sudo systemctl enable --now docker
sudo usermod -aG docker $USER
newgrp docker
```

## 4) Subir o backend

```bash
git clone https://github.com/Erivaldojelson/Visitas.git
cd Visitas/backend
cp .env.example .env
nano .env
docker compose up -d --build
```

## 5) Testar

```bash
curl http://localhost:8080/health
```

Do seu computador:

```bash
curl http://SEU_IP_PUBLICO:8080/health
```

## 6) Produção (recomendado)

- Use um domínio + TLS (Nginx/Caddy) e feche a porta `8080` para a internet.
- Configure `CARDS_PUBLIC_BASE_URL=https://api.seudominio.com` para que o QR Code aponte para a URL pública correta.

