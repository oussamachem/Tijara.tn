-- =====================================================================
-- V16 — Logo / photo de profil de la boutique.
-- URL relative (ex. /uploads/<uuid>.png), servie statiquement comme les images produits.
-- boutiques n'est PAS scopee par RLS : les grants table couvrent la nouvelle colonne.
-- =====================================================================
ALTER TABLE boutiques ADD COLUMN logo_url VARCHAR(500);
