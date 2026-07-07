# ---- Etape 1 : build du bundle statique (app CLIENT) ----
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
# Meme origine que nginx -> baseURL relative (les appels /api passent par le proxy).
ENV VITE_API_URL=""
RUN npm run build

# ---- Etape 2 : service statique + reverse proxy /api ----
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
