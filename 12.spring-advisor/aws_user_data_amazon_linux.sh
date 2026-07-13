#!/bin/bash
set -e
exec > >(tee /var/log/user-data.log) 2>&1
echo "=== INICIO USER DATA: $(date) ==="

# 1. Instalar Docker en Amazon Linux 2023
dnf update -y
dnf install -y docker
systemctl start docker
systemctl enable docker
usermod -aG docker ec2-user

# Instalar Docker Compose
mkdir -p /usr/local/lib/docker/cli-plugins
curl -SL https://github.com/docker/compose/releases/download/v2.27.0/docker-compose-linux-$(uname -m) -o /usr/local/lib/docker/cli-plugins/docker-compose
chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

# 2. Crear directorio del proyecto
mkdir -p /home/ec2-user/spring-advisor
cd /home/ec2-user/spring-advisor

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
chown -R ec2-user:ec2-user /home/ec2-user/spring-advisor

# 6. Descargar imágenes y levantar
docker compose pull
docker compose up -d

echo "=== DESPLIEGUE COMPLETADO: $(date) ==="
