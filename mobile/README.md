# Smart Boutique — Application mobile (vendeur)

Application **React Native (Expo SDK 51)** pour le rôle **VENDEUR** : vente par scan QR, vente manuelle, panier, consultation du stock, retours.

## Prérequis
- Node.js 18+
- Expo Go (sur le téléphone) **ou** un émulateur Android / simulateur iOS
- Le backend démarré et **accessible depuis le téléphone/émulateur**

## Configuration de l'URL backend
L'URL est injectée via `API_BASE_URL` (jamais codée en dur), lue dans `app.config.js`.

| Cible | `API_BASE_URL` |
|---|---|
| Émulateur Android | `http://10.0.2.2:8080` (défaut — `10.0.2.2` = localhost de l'hôte) |
| Simulateur iOS | `http://localhost:8080` |
| **Device physique** | `http://<IP_LAN_DE_VOTRE_MACHINE>:8080` (ex. `192.168.1.20`) |

> Sur device physique, `localhost` désigne le téléphone, pas votre PC : utilisez l'IP LAN, vérifiez que le backend écoute sur `0.0.0.0` et que le pare-feu autorise le port 8080.

```bash
cd mobile
cp .env.example .env        # adapter API_BASE_URL
npm install
API_BASE_URL=http://10.0.2.2:8080 npm start   # puis 'a' (Android) / 'i' (iOS), ou scanner le QR Expo Go
```

## Connexion
Compte **VENDEUR** (créé depuis le web admin → Vendeurs). Un compte ADMIN peut aussi se connecter (le backend tranche par 403 sur les écrans non permis).

## Écrans
- **Connexion** : JWT stocké dans `expo-secure-store` (Keychain/Keystore), survit au redémarrage.
- **Scanner** : caméra (`expo-camera` `CameraView`), lit le QR (= **référence** produit) → `GET /api/products/by-qr` → fiche produit → quantité → panier. Anti-rebond (un QR = un ajout), QR inconnu → message + re-scan.
- **Recherche** : vente manuelle par nom/référence → panier.
- **Panier** : lignes éditables, mode de paiement, remise (montant), validation → `POST /api/sales`. Bouton verrouillé pendant l'appel (anti double-vente), **jamais d'auto-retry**.
- **Reçu** : totaux **renvoyés par le serveur** (source de vérité), lignes, date, vendeur.
- **Stock** : liste/recherche en lecture, badges rupture/sous-seuil.
- **Retours** : « Mes ventes » (`GET /api/sales/mine`) → sélection d'une ligne → quantité + motif → `POST /api/returns` (plafond serveur-autoritatif).

## Décisions / périmètre
- **Token** : `expo-secure-store` (chiffré), pas AsyncStorage.
- **Argent** : le panier n'affiche qu'un **sous-total indicatif** ; le total contraignant vient **toujours** de la réponse serveur (prix capturé côté backend).
- **Gestion 401** : les endpoints `/api/auth/*` sont exclus de la logique « 401 → déconnexion » (un login raté affiche « identifiants invalides »).
- **Hors scope** (assumé) : **pas de mode hors-ligne** (une vente rejouée ré-décrémenterait un stock qui a bougé → incohérence) ; **pas d'impression thermique** (reçu écran suffisant).

## Notes techniques
- SDK Expo **51** choisi délibérément : API `CameraView`/`useCameraPermissions` moderne, et **évite** le bug de scan désactivé signalé sur des builds SDK 55. Le scan fonctionne en Expo Go et en build dev.
- React Navigation 6 (apparié à SDK 51) : npm signale « no longer supported » (des correctifs vont vers la v7) — sans impact fonctionnel sur cette cible.
- Si vous montez de SDK, réalignez les libs natives avec `npx expo install` (ne pas `npm install <lib>` à la main).
