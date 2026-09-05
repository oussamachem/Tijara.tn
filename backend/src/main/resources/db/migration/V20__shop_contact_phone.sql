-- =====================================================================
-- V20 — Numéro de contact WhatsApp de la boutique (public, non sensible).
-- Sert à construire un lien wa.me côté client (« Contacter sur WhatsApp »). Stocké au format
-- international (ex. +21612345678). Table boutiques = globale (grants), pas de RLS ici.
-- =====================================================================
ALTER TABLE boutiques ADD COLUMN contact_phone VARCHAR(20);
