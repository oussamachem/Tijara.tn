# ---- Etape 1 : build du bundle statique (app CLIENT) ----
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
# Meme origine que nginx -> baseURL relative (les appels /api passent par le proxy).
ENV VITE_API_URL=""
# URL PUBLIQUE de Keycloak (redirection login SSO). Prod : --build-arg VITE_KEYCLOAK_URL=https://...
ARG VITE_KEYCLOAK_URL=http://localhost:8081
ARG VITE_KEYCLOAK_REALM=smartboutique
ARG VITE_KEYCLOAK_CLIENT=smartboutique-web
ENV VITE_KEYCLOAK_URL=$VITE_KEYCLOAK_URL \
    VITE_KEYCLOAK_REALM=$VITE_KEYCLOAK_REALM \
    VITE_KEYCLOAK_CLIENT=$VITE_KEYCLOAK_CLIENT
RUN npm run build

# ---- Etape 2 : service statique + reverse proxy /api ----
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
