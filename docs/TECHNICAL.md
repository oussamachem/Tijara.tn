# Smart Boutique — Documentation technique

Application de gestion de stock et de vente par QR Code pour une boutique de vêtements.
Trois livrables : **API REST** (Spring Boot), **web admin** (React), **mobile vendeur** (React Native/Expo).

---

## 1. Architecture

```
            ┌─────────────────┐         ┌─────────────────┐
            │  Web admin       │         │  Mobile vendeur  │
            │  React + nginx   │         │  React Native    │
            └────────┬────────┘         └────────┬────────┘
                     │ HTTPS/JSON (JWT)           │ HTTP/JSON (JWT)
                     │  (nginx /api → backend)    │  (IP LAN → backend)
                     └─────────────┬──────────────┘
                                   ▼
                     ┌──────────────────────────────┐
                     │  API REST — Spring Boot 3.3   │
                     │  controller → service → repo  │
                     │  Sécurité JWT (stateless)     │
                     │  ZXing (QR), Flyway (prod)     │
                     └───────────────┬───────────────┘
                                     ▼
                     ┌──────────────────────────────┐
                     │  PostgreSQL 16                │
                     └──────────────────────────────┘
```

- **Backend** : architecture en couches `controller → service → repository → entity`, DTO en entrée/sortie (les entités JPA ne sont jamais exposées), `mapper` dédiés, gestion d'erreurs centralisée (`@RestControllerAdvice` → format `ApiError` unifié).
- **Sécurité** : JWT stateless (HS512), filtre `OncePerRequestFilter`, autorisations par rôle (`ADMIN` / `VENDEUR`), CORS configurable.
- **Web** : SPA React (Vite) servie par nginx, qui fait aussi reverse-proxy `/api` et `/uploads` vers le backend.
- **Mobile** : Expo (React Native), JWT en `expo-secure-store`, scan QR via `expo-camera`.

### Stack
| Couche | Technologies |
|---|---|
| Backend | Java 17, Spring Boot 3.3, Spring Web, Security, Data JPA, Validation, Actuator, Flyway, JJWT, ZXing |
| BDD | PostgreSQL 16 |
| Web | React 18, Vite 5, React Router 6, Tailwind 3, Axios |
| Mobile | Expo SDK 51, React Navigation 6, TanStack Query, expo-camera, expo-secure-store |
| Tests | JUnit 5, Spring Boot Test, **Testcontainers (PostgreSQL réel)** |
| Conteneurisation | Docker, docker-compose (postgres + backend + nginx) |

---

## 2. Modèle de données

```
users (1)───< sales (1)───< sale_items >───(N) products >───(N) categories
   │             │                                │
   │             └──────< returns >───────────────┘
   └──< password_reset_tokens
```

| Table | Champs clés |
|---|---|
| `users` | id, full_name, email (unique), password (BCrypt), role (ADMIN/VENDEUR), active, created_at |
| `categories` | id, name (unique), description |
| `products` | id, reference (unique), name, description, category_id (FK), size, color, purchase_price, sale_price, quantity, seuil_alerte, image_url, qr_code (unique), created_at |
| `sales` | id, seller_id (FK users), sale_date, payment_method (ESPECES/CARTE/MIXTE), discount, total_amount |
| `sale_items` | id, sale_id (FK), product_id (FK), quantity, unit_price, total_price |
| `returns` | id, sale_id (FK), product_id (FK), quantity, reason, return_date |
| `password_reset_tokens` | id, token (unique), user_id (FK), expires_at, used, created_at |

Montants en `NUMERIC(12,2)` (jamais de `double`). Schéma figé dans `backend/.../db/migration/V1__init.sql` (Flyway, prod).

---

## 3. Endpoints (API REST)

Auth par `Authorization: Bearer <JWT>`. Format d'erreur : `{ timestamp, status, error, message, path }`.

| Méthode | Endpoint | Accès |
|---|---|---|
| POST | `/api/auth/login` | public |
| POST | `/api/auth/forgot-password` · `/api/auth/reset-password` | public |
| GET/PUT | `/api/profile` · PUT `/api/profile/password` | authentifié |
| GET | `/api/categories` · `/api/categories/{id}` | authentifié |
| POST/PUT/DELETE | `/api/admin/categories[/{id}]` | ADMIN |
| GET | `/api/products` (recherche paginée) · `/{id}` · `/low-stock` · `/by-qr?code=` · `/{id}/qrcode` | authentifié |
| POST/PUT/DELETE | `/api/admin/products[/{id}]` | ADMIN |
| POST | `/api/admin/products/{id}/image` (upload) | ADMIN |
| PATCH | `/api/admin/products/{id}/stock` · `/stock/adjust` | ADMIN |
| POST | `/api/sales` | ADMIN, VENDEUR |
| GET | `/api/sales/{id}` | authentifié |
| GET | `/api/sales/mine` | ADMIN, VENDEUR (filtré par token) |
| POST | `/api/returns` | ADMIN, VENDEUR |
| GET | `/api/admin/sales` · `/api/admin/returns` (historique) | ADMIN |
| GET | `/api/dashboard` | ADMIN |
| GET | `/uploads/{fichier}` · `/actuator/health` · `/actuator/info` | public |

---

## 4. Déploiement

### Production (docker-compose : postgres + backend + nginx)
```bash
cd smart-boutique
cp .env.example .env          # renseigner POSTGRES_PASSWORD, JWT_SECRET, ADMIN_PASSWORD, CORS_ALLOWED_ORIGINS
docker compose up --build -d
```
- Web : `http://localhost:8090` (mapper 80/443 + TLS en prod réelle).
- Backend non exposé (joint via nginx). PostgreSQL exposé sur 5544 (interne `postgres:5432`).
- Profil `prod` actif : `ddl-auto=validate` + **Flyway** applique `V1__init.sql`, pas de stack trace dans les réponses, Actuator limité à `health`/`info`, pas de données de démo.
- **Volumes** : `pg_data` (BDD) et `uploads_data` (images produits) — persistants.

### Variables d'environnement (compose)
`POSTGRES_PASSWORD`*, `JWT_SECRET`* (≥ 32 car.), `ADMIN_PASSWORD`*, `POSTGRES_DB`, `POSTGRES_USER`, `ADMIN_EMAIL`, `CORS_ALLOWED_ORIGINS`, `JWT_EXPIRATION_MS`. (* = requis, pas de défaut.)

### Mobile (hors compose)
Artefact Expo, buildé séparément :
```bash
cd mobile && npm install
API_BASE_URL=https://api.votre-domaine npx expo start      # dev
# build APK/AAB : eas build -p android   (compte Expo/EAS requis)
```

### Dev (sans Docker pour le backend)
`docker compose up -d postgres` puis `cd backend && SERVER_PORT=8089 mvn spring-boot:run` ; `cd web && npm run dev`.

---

## 5. Tests

Suite d'intégration **100 % PostgreSQL réel** via **Testcontainers** (H2 retiré : il masquait des divergences SQL, cf. registre §6). Base commune `AbstractPostgresIT` (conteneur singleton réutilisé). Couverture clé :
- auth/rôles (login, compte désactivé 403, accès admin refusé 403, reset token expiré) ;
- produits/catégories (QR auto, doublon 409, suppression catégorie liée 409, recherche paginée, image PNG) ;
- ventes/retours (décrément, **rollback tout-ou-rien**, capture prix, remise, retour borné) ;
- **concurrence** (dernier exemplaire 2 threads → 1 succès ; 20 ventes // stock 10 → 10/10) ;
- **frontière de journée minuit `Africa/Tunis`**.

```bash
# CI avec Docker compatible Testcontainers :
cd backend && mvn test
# Sur cette machine (incompatibilité docker-java ↔ Docker Desktop) : pointer un Postgres externe
SB_TEST_DB_URL=jdbc:postgresql://localhost:5544/sbtest SB_TEST_DB_USER=smartboutique SB_TEST_DB_PASSWORD=smartboutique mvn test
```

---

## 6. Registre des tradeoffs conscients

| Décision | Choix | Justification / risque assumé |
|---|---|---|
| JWT stateless | Pas de blocklist de révocation | Simplicité ; un token reste valide jusqu'à expiration. Mitigation : durée de vie courte, compte désactivé bloqué à la connexion. |
| Stockage token web | `localStorage` | Exposition XSS théorique ; SPA sans backend de session. Mobile utilise `expo-secure-store` (chiffré). |
| Stockage token mobile | `expo-secure-store` (Keychain/Keystore) | Sécurisé, pas de credential en clair. |
| Images produits | Fichier disque + URL publique (UUID) sous `/uploads` | Garde la BDD légère, cache HTTP ; chemin non devinable (UUID), pas de listing. Volume Docker pour la persistance. |
| Remise | **Montant fixe par vente**, soustrait du sous-total | Sémantique unique et serveur-autoritaire ; total négatif refusé (400). |
| Anti-survente | `UPDATE ... WHERE quantity >= :q` atomique | Pas de verrou applicatif long ; prouvé par test de concurrence sur Postgres réel. |
| Prix de vente | Capturé (`unit_price`) au moment de la vente | Un changement de prix ultérieur ne réécrit pas l'historique. |
| CA dashboard | **NET** : ventes du jour − valeur des retours du jour | Retours valorisés au prix capturé. Affiche brut/retours/net pour transparence. |
| Frontière de journée | Fuseau **Africa/Tunis** | « Ventes du jour » alignées sur le jour métier tunisien, indépendamment du fuseau du conteneur (UTC). |
| Top produits | Par quantité brute vendue (`GROUP BY`) | KPI volume, indépendant des retours. |
| `/api/sales/mine` | Ajout backend (Phase 7) | Aucun endpoint VENDOR ne listait ses ventes (historique ADMIN-only) ; nécessaire à l'écran Retour mobile. |
| Offline mobile | **Hors scope** | Une vente rejouée hors-ligne re-décrémenterait un stock qui a bougé → incohérence / double-vente. La cohérence prime. |
| Impression thermique | **Hors scope** | Reçu écran + partage suffisent pour le périmètre. |
| Testcontainers ↔ Docker local | Trappe `SB_TEST_DB_URL` | Le client docker-java embarqué ne négocie pas avec ce Docker Desktop (API 1.54) ; en CI standard Testcontainers démarre seul. |
