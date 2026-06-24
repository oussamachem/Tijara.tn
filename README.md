# Smart Boutique

Application de **gestion de stock et de vente par QR Code** pour une boutique de vêtements.

Le projet est un **monorepo** composé de trois parties :

| Dossier | Rôle | Stack |
|---|---|---|
| [`backend/`](backend/) | API REST | Java 17, Spring Boot 3.x, Spring Data JPA, PostgreSQL, JWT, ZXing |
| [`web/`](web/) | App admin | React (Vite), Tailwind CSS, Axios *(Phase 6)* |
| [`mobile/`](mobile/) | App vendeur | React Native (Expo) *(Phase 7)* |

---

## État d'avancement

Le développement suit un **plan en 8 phases**. État actuel :

- [x] **Phase 1 — Initialisation** : structure du monorepo, projet Spring Boot, connexion PostgreSQL, docker-compose de base, entités JPA + relations, seed du compte admin.
- [x] **Phase 2 — Authentification & sécurité** : JWT (stateless), login, rôles ADMIN/VENDEUR, mot de passe oublié/réinitialisation, gestion des vendeurs, CORS.
- [x] **Phase 3 — Catégories & produits** : CRUD, recherche paginée, génération QR Code (ZXing), endpoint image QR, upload d'image.
- [x] **Phase 4 — Stock, ventes & retours** : logique transactionnelle, vente par QR/manuelle, anti-survente concurrente, retours, alertes de seuil.
- [x] **Phase 5 — Tableau de bord & historique** : statistiques agrégées (CA net), historique des ventes/retours filtrable, agrégations SQL, pas de N+1.
- [x] **Phase 6 — Application web React** : toutes les pages admin (connexion, dashboard, produits + impression QR, catégories, vendeurs, historique).
- [x] **Phase 7 — Application mobile React Native** : écrans vendeur (connexion, scan QR, vente manuelle, panier/validation, stock, retours).
- [x] **Phase 8 — Durcissement & conteneurisation** : tests Testcontainers (PostgreSQL réel), config prod (Flyway + `validate`), docker-compose complet (postgres + backend + nginx), documentation.

---

## Démarrage rapide (Docker — production)

Prérequis : **Docker** + **Docker Compose**.

```bash
cd smart-boutique
cp .env.example .env        # RENSEIGNER : POSTGRES_PASSWORD, JWT_SECRET, ADMIN_PASSWORD, CORS_ALLOWED_ORIGINS
docker compose up --build -d
```

Stack lancée : **postgres + backend (profil prod) + nginx (web)**.
- **Web admin** : `http://localhost:8090` (nginx ; mapper 80/443 + TLS en prod réelle).
- **Backend** : non exposé sur l'hôte — joint via nginx (`/api`, `/uploads`).
- **PostgreSQL** : `localhost:5544` (interne `postgres:5432`).
- Données persistées sur volumes `pg_data` (BDD) et `uploads_data` (images).

En prod, Flyway crée le schéma (`V1__init.sql`) et Hibernate le **valide** (`ddl-auto=validate`). Un **compte administrateur** est créé (`ADMIN_EMAIL` / `ADMIN_PASSWORD`).

📄 Documentation : [docs/TECHNICAL.md](docs/TECHNICAL.md) (architecture, modèle, endpoints, déploiement, registre des tradeoffs) · [docs/USER_GUIDE.md](docs/USER_GUIDE.md) (guides admin & vendeur).

---

## Démarrage en mode développement

### Backend (Spring Boot)

Prérequis : **JDK 17+**, **Maven 3.9+**, une instance **PostgreSQL** accessible.

1. Démarrer PostgreSQL seul (le plus simple) :
   ```bash
   docker compose up -d postgres
   ```
2. Lancer le backend :
   ```bash
   cd backend
   # variables par défaut : localhost:5544 (conteneur Docker) / smartboutique / smartboutique
   mvn spring-boot:run
   ```
   > Si vous préférez utiliser un PostgreSQL **installé localement** (port 5432), créez-y la base et le rôle correspondants puis surchargez `DB_URL=jdbc:postgresql://localhost:5432/smart_boutique`.

L'API démarre sur `http://localhost:8080`.

### Web (admin)

```bash
cd web
cp .env.example .env      # VITE_API_URL=http://localhost:8080
npm install
npm run dev               # http://localhost:5173
```
Connexion avec un compte **ADMIN**. Détails : [`web/README.md`](web/README.md).

### Mobile (vendeur)

```bash
cd mobile
cp .env.example .env       # API_BASE_URL (10.0.2.2:8080 émulateur Android, IP LAN sur device)
npm install
npm start                  # 'a' Android / 'i' iOS / QR Expo Go
```
Connexion avec un compte **VENDEUR**. Détails : [`mobile/README.md`](mobile/README.md).

---

## Compte administrateur par défaut

| Champ | Valeur par défaut (modifiable via `.env`) |
|---|---|
| Email | `admin@smartboutique.com` |
| Mot de passe | `Admin@123` |

> ⚠️ À changer impérativement en production. Le mot de passe est stocké haché (BCrypt) et affiché en clair dans les logs uniquement au moment du seed initial.

---

## API — Authentification & utilisateurs (Phase 2)

Authentification par **JWT** (header `Authorization: Bearer <token>`). Le token contient `id`, `email`, `role` ; secret et durée de vie externalisés (`JWT_SECRET`, `JWT_EXPIRATION_MS`).

| Méthode | Endpoint | Accès | Description |
|---|---|---|---|
| POST | `/api/auth/login` | public | Connexion → `{ token, tokenType, user }`. Compte désactivé → **403**. |
| POST | `/api/auth/forgot-password` | public | Génère un token de reset (exp. 30 min), email simulé dans les logs. |
| POST | `/api/auth/reset-password` | public | Réinitialise via token (validé : existant, non expiré, non utilisé). |
| GET | `/api/profile` | authentifié | Profil de l'utilisateur connecté. |
| PUT | `/api/profile` | authentifié | Mise à jour du profil (nom, email). |
| PUT | `/api/profile/password` | authentifié | Changement de mot de passe (vérifie l'ancien). |
| GET | `/api/admin/sellers` | ADMIN | Liste des vendeurs. |
| POST | `/api/admin/sellers` | ADMIN | Création d'un vendeur (BCrypt). |
| GET/PUT | `/api/admin/sellers/{id}` | ADMIN | Détail / modification. |
| PATCH | `/api/admin/sellers/{id}/deactivate` | ADMIN | Désactivation (`active=false`). |
| PATCH | `/api/admin/sellers/{id}/activate` | ADMIN | Réactivation. |

Règles d'autorisation : `/api/admin/**` → **ADMIN** ; toute autre route → authentifié (ADMIN ou VENDEUR). Erreurs `401` (non authentifié) et `403` (droits insuffisants) renvoyées au format `ApiError`.

## API — Catégories & produits (Phase 3)

| Méthode | Endpoint | Accès | Description |
|---|---|---|---|
| GET | `/api/categories` | authentifié | Liste des catégories. |
| GET | `/api/categories/{id}` | authentifié | Détail d'une catégorie. |
| POST | `/api/admin/categories` | ADMIN | Création. |
| PUT | `/api/admin/categories/{id}` | ADMIN | Modification. |
| DELETE | `/api/admin/categories/{id}` | ADMIN | Suppression — **409** si des produits y sont rattachés. |
| GET | `/api/products` | authentifié | Liste **paginée** + filtres `name`, `reference`, `categoryId` (params `page`, `size`, `sort`). |
| GET | `/api/products/{id}` | authentifié | Détail d'un produit. |
| GET | `/api/products/{id}/qrcode` | authentifié | **Image PNG** du QR Code (impression web). |
| POST | `/api/admin/products` | ADMIN | Création — référence unique (**409** sinon) + **QR Code généré** auto. |
| PUT | `/api/admin/products/{id}` | ADMIN | Modification (audit du changement de prix). |
| DELETE | `/api/admin/products/{id}` | ADMIN | Suppression (tracée). |
| POST | `/api/admin/products/{id}/image` | ADMIN | Upload image (`multipart/form-data`, champ `file`). |
| GET | `/uploads/{fichier}` | public | Image produit servie statiquement. |

**QR Code** : à la création, le champ `qr_code` encode la **`reference`** du produit (contenu stable et signifiant, plutôt que l'id auto-incrémenté). L'endpoint `/api/products/{id}/qrcode` régénère l'image PNG à la volée via **ZXing**.

## API — Stock, ventes & retours (Phase 4)

| Méthode | Endpoint | Accès | Description |
|---|---|---|---|
| PATCH | `/api/admin/products/{id}/stock` | ADMIN | Définit la quantité absolue (inventaire). |
| PATCH | `/api/admin/products/{id}/stock/adjust` | ADMIN | Ajuste de `delta` (refuse un stock négatif). |
| GET | `/api/products/low-stock` | authentifié | Produits en rupture (qty=0) ou sous le seuil (qty ≤ seuil). |
| GET | `/api/products/by-qr?code=<reference>` | authentifié | Détails d'un produit par contenu de QR Code (vente par scan). |
| POST | `/api/sales` | ADMIN, VENDEUR | Crée une vente (1..n articles). Le vendeur = utilisateur connecté. |
| GET | `/api/sales/{id}` | authentifié | Détail d'une vente. |
| GET | `/api/sales/mine` | ADMIN, VENDEUR | Historique paginé des ventes **de l'utilisateur connecté** (filtré par le token ; utilisé par le mobile pour choisir un retour). |
| POST | `/api/returns` | ADMIN, VENDEUR | Enregistre un retour (réintègre le stock). |

**Sémantique des ventes**
- **Transaction tout-ou-rien** (`@Transactional`) : vente + lignes + décrément stock dans une seule transaction ; toute erreur → rollback complet.
- **Stock insuffisant** → **409** en nommant le produit concerné ; rien n'est enregistré.
- **Prix figé** : `unit_price` est copié depuis le produit au moment de la vente (un changement de prix ultérieur ne réécrit pas l'historique). Calculs en `BigDecimal`.
- **Remise** : **montant fixe** (même devise) soustrait du sous-total. Un `total_amount` négatif est refusé (**400**).
- **Retours** : impossible de retourner plus que `vendu − déjà retourné` sur la vente (**409**).

## API — Tableau de bord & historique (Phase 5)

| Méthode | Endpoint | Accès | Description |
|---|---|---|---|
| GET | `/api/dashboard` | ADMIN | Indicateurs : nb produits, stock total, produits sous seuil, ventes du jour (nombre + **CA net**), top produits. |
| GET | `/api/admin/sales` | ADMIN | Historique paginé des ventes ; filtres `from`/`to` (yyyy-MM-dd, inclus) et `sellerId`. |
| GET | `/api/sales/{id}` | authentifié | Détail d'une vente (lignes, prix unitaires capturés, remise, total, vendeur, date). |
| GET | `/api/admin/returns` | ADMIN | Historique paginé des retours ; filtres `from`/`to`. |

**Cohérence & performance**
- **Chiffre d'affaires NET** : `CA net du jour = Σ total_amount des ventes du jour − valeur des retours du jour`. La valeur d'un retour est calculée au **prix unitaire capturé** sur la vente d'origine (jointure `Return × SaleItem`). Le dashboard expose `todayGrossRevenue`, `todayReturnsValue` et `todayNetRevenue` pour la transparence.
- **Top produits** : par quantité brute vendue (`GROUP BY` en SQL).
- **Agrégations en base** : `COUNT`/`SUM`/`GROUP BY` (jamais de calcul en mémoire) ; montants en `BigDecimal`.
- **Pas de N+1** : la liste d'historique utilise une **projection légère** (sans charger les lignes) ; le détail d'une vente est chargé via **`@EntityGraph`** (vendeur + lignes + produits).

## Configuration (variables d'environnement)

Toute la configuration sensible est externalisée. Voir [`.env.example`](.env.example).

| Variable | Description | Défaut (dev) |
|---|---|---|
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | Base PostgreSQL | `smart_boutique` / `smartboutique` / `smartboutique` |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | Connexion JDBC du backend | `jdbc:postgresql://...` |
| `JPA_DDL_AUTO` | Stratégie de schéma Hibernate | `update` |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` / `ADMIN_NAME` | Compte admin seedé | voir ci-dessus |
| `JWT_SECRET` / `JWT_EXPIRATION_MS` | Paramètres JWT (Phase 2) | — |

---

## Modèle de données

```
users (1) ──< sales (1) ──< sale_items >── (N) products >── (N) categories
                  │                              │
                  └──< returns >─────────────────┘
```

Tables : `users`, `categories`, `products`, `sales`, `sale_items`, `returns`.
Détail des champs : voir les entités dans [`backend/src/main/java/com/smartboutique/entity/`](backend/src/main/java/com/smartboutique/entity/).

---

## Architecture backend

Architecture en couches : `controller` → `service` → `repository` → `entity`, avec des **DTO** en entrée/sortie (les entités JPA ne sont jamais exposées) et des `mapper` dédiés. La gestion d'erreurs est centralisée dans un `@RestControllerAdvice` ([`GlobalExceptionHandler`](backend/src/main/java/com/smartboutique/exception/GlobalExceptionHandler.java)) qui renvoie un format JSON unifié :

```json
{
  "timestamp": "2026-06-21T12:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Produit introuvable (id=42)",
  "path": "/api/products/42"
}
```

---

## Choix techniques documentés

- **Lombok** : utilisé pour réduire le boilerplate (getters/setters/builders). Outil de build standard de l'écosystème Spring, sans impact à l'exécution.
- **`spring-security-crypto` (seul) en Phase 1** : permet le hachage BCrypt du mot de passe admin sans activer toute la chaîne de sécurité web, qui sera mise en place en Phase 2.
- **Schéma de base via `hibernate.ddl-auto=update`** : suffisant et standard pour le développement ; le seed des données (`DataSeeder`) est idempotent. Un outil de migration dédié pourra être introduit pour la production.
- **Concurrence des ventes (anti-survente)** : le stock est décrémenté via une **mise à jour atomique conditionnelle** `UPDATE products SET quantity = quantity - :q WHERE id = :id AND quantity >= :q`. Si 0 ligne n'est affectée, le stock est insuffisant (y compris en cas de vente concurrente) → **409** + rollback. Choix retenu plutôt qu'un verrou pessimiste car il garantit l'atomicité au niveau BDD sans maintenir de verrou applicatif long ; les lignes d'une vente sont traitées triées par id produit pour limiter les interblocages. *(Preuve : 20 ventes simultanées d'1 unité sur un stock de 10 → exactement 10 succès, 10 × 409, stock final 0.)*
- **Stockage des images** : **disque local** dans le dossier `uploads/` (configurable via `UPLOADS_DIR`), servi statiquement sous `/uploads/**` et monté sur un volume Docker pour la persistance. Choix retenu plutôt que le base64 en base pour garder la BDD légère et permettre la mise en cache HTTP des images. Validation à l'upload : types PNG/JPG/WEBP, taille max 5 Mo ; nom de fichier généré (UUID) pour éviter les collisions.
```
