#!/bin/bash
set -e
exec > >(tee /var/log/user-data.log) 2>&1
echo "=== INICIO USER DATA: $(date) ==="

# 1. Instalar Docker
apt-get update -y
apt-get install -y ca-certificates curl gnupg
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
chmod a+r /etc/apt/keyrings/docker.asc
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  tee /etc/apt/sources.list.d/docker.list > /dev/null
apt-get update -y
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
systemctl start docker
systemctl enable docker
usermod -aG docker ubuntu

# 2. Crear directorio del proyecto
mkdir -p /home/ubuntu/spring-advisor
cd /home/ubuntu/spring-advisor

# 3. Crear archivo .env con la API Key
cat > .env << 'EOF'
GEMINI_API_KEY=AIzaSyDhNnQhY_UaTcplOlHaRjekm1tyTAAzIxY
EOF

# 4. Crear docker-compose.yml
cat > docker-compose.yml << 'EOF'
version: '3.8'
name: spring-advisor

services:
  qdrant:
    image: qdrant/qdrant:latest
    container_name: qdrant-db
    ports:
      - "6333:6333"
      - "6334:6334"
    volumes:
      - qdrant_data:/qdrant/storage
    networks:
      - app-network
    restart: unless-stopped

  backend:
    image: dariovergara/ucegemini:backend
    container_name: spring-advisor-backend
    depends_on:
      - qdrant
    environment:
      - SPRING_AI_VECTORSTORE_QDRANT_HOST=qdrant
      - SPRING_AI_VECTORSTORE_QDRANT_PORT=6334
      - APP_KEY=${GEMINI_API_KEY}
    ports:
      - "8091:8091"
    networks:
      - app-network
    restart: unless-stopped

  frontend:
    image: dariovergara/ucegemini:frontend
    container_name: spring-advisor-frontend
    depends_on:
      - backend
    ports:
      - "80:80"
    networks:
      - app-network
    restart: unless-stopped

  cloudflared:
    image: cloudflare/cloudflared:latest
    container_name: spring-advisor-tunnel
    command: tunnel --url http://frontend:80
    depends_on:
      - frontend
    networks:
      - app-network
    restart: unless-stopped

networks:
  app-network:
    driver: bridge

volumes:
  qdrant_data:
EOF

# 5. Asignar permisos
chown -R ubuntu:ubuntu /home/ubuntu/spring-advisor

# 6. Descargar imágenes y levantar
docker compose pull
docker compose up -d

echo "=== DESPLIEGUE COMPLETADO: $(date) ==="
