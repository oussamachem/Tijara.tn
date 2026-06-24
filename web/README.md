# Smart Boutique — Application web (admin)

Interface d'administration React (Vite + Tailwind + React Router + Axios).

## Prérequis
- Node.js 18+ et npm
- Le backend démarré et accessible (par défaut `http://localhost:8080`)

## Configuration
Copier `.env.example` en `.env` et adapter si besoin :

```
VITE_API_URL=http://localhost:8080
```

> Le backend autorise par défaut l'origine `http://localhost:5173` (CORS).

## Démarrage en développement
```bash
cd web
npm install
npm run dev
```
L'application est servie sur `http://localhost:5173`.

## Build de production
```bash
npm run build      # génère dist/
npm run preview    # sert le build localement
```

## Connexion
Se connecter avec un compte **ADMIN** (par défaut `admin@smartboutique.com` / `Admin@123`).
L'accès est refusé aux comptes vendeurs (réservés à l'application mobile).

## Pages
- **Connexion** + mot de passe oublié / réinitialisation
- **Tableau de bord** : cartes de statistiques, CA net du jour, produits sous seuil, meilleures ventes
- **Produits** : recherche/filtre, CRUD, upload d'image, affichage + **impression du QR Code**
- **Catégories** : CRUD (suppression bloquée si des produits y sont rattachés)
- **Vendeurs** : CRUD + activation/désactivation
- **Historique** : ventes (filtres date + vendeur) avec détail, et vue des retours

## Architecture
```
src/
├── api/            # client Axios (intercepteur JWT, redirection 401) + endpoints
├── context/        # AuthContext (session JWT)
├── components/     # UI réutilisable, Layout, ProtectedRoute
├── pages/          # une page par écran
└── utils/          # formatage (montants, dates)
```
Le JWT est stocké dans `localStorage` ; un intercepteur Axios ajoute l'en-tête `Authorization`
et redirige vers `/login` en cas de `401`. Les routes admin sont protégées par `ProtectedRoute`.
