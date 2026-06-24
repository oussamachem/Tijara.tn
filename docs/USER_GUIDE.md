# Smart Boutique — Guide utilisateur

Deux applications : **Web (administrateur)** et **Mobile (vendeur)**.

---

## A. Application web — Administrateur

Accès : navigateur, URL fournie par l'administrateur système (en local : `http://localhost:8090`).
Connexion avec un compte **ADMIN** (par défaut `admin@smartboutique.com`). Les comptes vendeurs sont refusés sur le web (réservés au mobile).

### Connexion / mot de passe oublié
- Saisir email + mot de passe. Un mauvais mot de passe affiche « Identifiants invalides ».
- « Mot de passe oublié » → saisir l'email → un lien de réinitialisation est généré (en dev, affiché dans les logs serveur). Saisir le token reçu + le nouveau mot de passe.

### Tableau de bord
Vue d'ensemble : nombre de produits, stock total, produits sous le seuil/en rupture, ventes du jour, **chiffre d'affaires net** (ventes − retours), produits les plus vendus, liste des produits à réapprovisionner.

### Produits
- **Rechercher/filtrer** par nom, référence, catégorie.
- **Ajouter / modifier** un produit (référence unique, prix, quantité, seuil d'alerte, catégorie). Le **QR Code est généré automatiquement** à la création.
- **Image** : téléverser une image (PNG/JPG/WEBP, max 5 Mo).
- **QR Code** : bouton 🔳 → aperçu + **🖨️ Imprimer** (à coller sur le produit ; il sera scanné par le vendeur).
- **Supprimer** un produit.

### Catégories
Créer / modifier / supprimer. La suppression est refusée si des produits sont rattachés à la catégorie.

### Vendeurs
Créer un vendeur (nom, email, mot de passe), modifier, **désactiver / réactiver**. Un compte désactivé ne peut plus se connecter (web ou mobile).

### Historique
- Onglet **Ventes** : filtrer par période et par vendeur ; cliquer une vente pour le **détail** (lignes, prix unitaires, remise, total).
- Onglet **Retours** : liste filtrable par période.

---

## B. Application mobile — Vendeur

Installer l'app (APK fourni ou via Expo). Au premier lancement, renseigner l'URL du serveur si demandé. Se connecter avec un compte **VENDEUR** créé par l'administrateur.

### Vendre par scan (onglet Scanner)
1. Autoriser la caméra.
2. Viser le **QR Code** du produit. L'app affiche la fiche (nom, prix, stock).
3. Choisir la quantité → **Ajouter au panier**.
4. Si le QR est inconnu : message « produit introuvable » → « Scanner à nouveau ».

### Vente manuelle (onglet Recherche)
Rechercher par nom ou référence → toucher un résultat → quantité → panier. (Utile si le QR est abîmé.)

### Panier / validation (onglet Panier)
- Ajuster les quantités, retirer des lignes.
- Choisir le **mode de paiement** (Espèces / Carte / Mixte) et une **remise** éventuelle (montant).
- **Valider la vente** → un **reçu** s'affiche avec le total calculé par le serveur. Le stock est décrémenté.
- Si un article n'a plus assez de stock, un message indique le produit concerné ; corriger la quantité et revalider.

### Consultation du stock (onglet Stock)
Liste et recherche des produits avec quantité et état (OK / sous seuil / rupture). Lecture seule.

### Retour (onglet Retours)
1. Choisir une de **vos ventes** récentes.
2. Sélectionner l'article à retourner, la **quantité** et le **motif**.
3. Valider → le stock est réintégré. Un retour supérieur à la quantité vendue (moins déjà retournée) est refusé.

---

## Compte administrateur par défaut
`admin@smartboutique.com` / mot de passe défini au déploiement (`ADMIN_PASSWORD`). **À changer après la première connexion.**
