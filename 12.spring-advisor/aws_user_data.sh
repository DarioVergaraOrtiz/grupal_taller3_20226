#!/bin/bash
# ==============================================================================
# AWS EC2 User Data Script
# OS Target: Ubuntu 22.04 / 24.04 LTS
# Proposito: Instalar Docker y Docker Compose automáticamente al iniciar la instancia
# ==============================================================================

# 1. Actualizar el sistema e instalar dependencias
apt-get update -y
apt-get upgrade -y
apt-get install -y ca-certificates curl gnupg git

# 2. Agregar la clave GPG oficial de Docker
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
chmod a+r /etc/apt/keyrings/docker.asc

# 3. Configurar el repositorio estable de Docker
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  tee /etc/apt/sources.list.d/docker.list > /dev/null

# 4. Instalar Docker Engine y el plugin Docker Compose
apt-get update -y
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# 5. Iniciar y habilitar el servicio Docker
systemctl start docker
systemctl enable docker

# 6. Otorgar permisos al usuario 'ubuntu' para ejecutar docker sin sudo
usermod -aG docker ubuntu

# ==============================================================================
# DESPLIEGUE AUTOMÁTICO (Opcional)
# ==============================================================================
# Descomenta las siguientes líneas y añade la URL de tu repositorio 
# si deseas que el servidor clone y despliegue el proyecto automáticamente.

# cd /home/ubuntu
# git clone https://github.com/DarioVergaraOrtiz/grupal_taller3_20226.git
# cd grupal_taller3_20226/12.spring-advisor
# docker compose up --build -d
