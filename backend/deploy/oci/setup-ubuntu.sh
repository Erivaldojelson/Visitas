#!/usr/bin/env bash
set -euo pipefail

sudo apt-get update
sudo apt-get install -y docker.io docker-compose-plugin git
sudo systemctl enable --now docker

if ! groups "$USER" | grep -q '\bdocker\b'; then
  sudo usermod -aG docker "$USER"
fi

echo "Docker instalado. Refaça login (ou rode 'newgrp docker') para usar docker sem sudo."

