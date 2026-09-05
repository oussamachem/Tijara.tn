-- =====================================================================
-- V21 — Message WhatsApp par défaut de la boutique (public, non sensible).
-- Préfixe du message pré-rempli du lien wa.me côté client. Nullable -> fallback applicatif si vide.
-- Table boutiques = globale (grants), pas de RLS ici.
-- =====================================================================
ALTER TABLE boutiques ADD COLUMN whatsapp_default_message VARCHAR(500);
