# ---- Etape 1 : build du bundle statique ----
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
# Build derriere nginx : API en meme origine -> baseURL relative (VITE_API_URL vide).
ENV VITE_API_URL=""
RUN npm run build

# ---- Etape 2 : service statique + reverse proxy ----
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
