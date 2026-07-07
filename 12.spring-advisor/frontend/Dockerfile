# Stage 1: Build the React application
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
# Construimos la aplicación
RUN npm run build

# Stage 2: Serve the application with Nginx
FROM nginx:alpine
# Copiamos la configuración personalizada de Nginx
COPY nginx.conf /etc/nginx/conf.d/default.conf
# Copiamos los archivos estáticos construidos desde la etapa anterior
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
