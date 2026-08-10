-- =====================================================================
-- V18 — Livraison à domicile + intégration transporteur Goodex (Neo Parcel).
--   * users      : coordonnées de livraison du CLIENT (saisies à l'inscription / dans le profil).
--   * orders     : SNAPSHOT livraison (figé à la commande) + suivi transporteur (ean + statut).
--   * boutiques  : identifiants Goodex par boutique (token permanent + user_id + base URL).
-- users/boutiques ne sont PAS scopés par RLS (grants table). orders l'est déjà (V12) : les
-- nouvelles colonnes sont de simples données, aucune policy à modifier.
-- =====================================================================

-- Coordonnées de livraison par défaut du client (réutilisées à chaque commande).
ALTER TABLE users ADD COLUMN phone      VARCHAR(30);
ALTER TABLE users ADD COLUMN address    VARCHAR(300);
ALTER TABLE users ADD COLUMN governorat VARCHAR(40);

-- Snapshot livraison (figé au moment de la commande) + suivi Goodex.
ALTER TABLE orders ADD COLUMN delivery_name       VARCHAR(150);
ALTER TABLE orders ADD COLUMN delivery_phone      VARCHAR(30);
ALTER TABLE orders ADD COLUMN delivery_address    VARCHAR(300);
ALTER TABLE orders ADD COLUMN delivery_governorat VARCHAR(40);
ALTER TABLE orders ADD COLUMN carrier_ean         VARCHAR(60);   -- code de suivi Goodex (ean)
ALTER TABLE orders ADD COLUMN carrier_status      VARCHAR(60);   -- dernier statut Goodex connu
ALTER TABLE orders ADD COLUMN carrier_status_at   TIMESTAMP;     -- date du dernier rafraîchissement

-- Identifiants Goodex de la boutique (token permanent ; user_id = expéditeur ; base URL configurable).
ALTER TABLE boutiques ADD COLUMN goodex_token    VARCHAR(255);
ALTER TABLE boutiques ADD COLUMN goodex_user_id  VARCHAR(60);
ALTER TABLE boutiques ADD COLUMN goodex_base_url VARCHAR(200);
