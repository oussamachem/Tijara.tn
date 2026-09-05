# ---- Etape 1 : build du bundle statique ----
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
# Build derriere nginx : API en meme origine -> baseURL relative (VITE_API_URL vide).
ENV VITE_API_URL=""
# URL PUBLIQUE de Keycloak (le navigateur y est redirige pour le login SSO). En prod, passer
# --build-arg VITE_KEYCLOAK_URL=https://auth.mondomaine.com. Defaut : Keycloak expose en local.
ARG VITE_KEYCLOAK_URL=http://localhost:8081
ARG VITE_KEYCLOAK_REALM=smartboutique
ARG VITE_KEYCLOAK_CLIENT=smartboutique-web
ENV VITE_KEYCLOAK_URL=$VITE_KEYCLOAK_URL \
    VITE_KEYCLOAK_REALM=$VITE_KEYCLOAK_REALM \
    VITE_KEYCLOAK_CLIENT=$VITE_KEYCLOAK_CLIENT
RUN npm run build

# ---- Etape 2 : service statique + reverse proxy ----
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
