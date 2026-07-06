-- =====================================================================
-- Multi-tenancy Phase 2 — Espace SUPER_ADMIN.
-- Le SUPER_ADMIN cree / suspend / reactive des boutiques au RUNTIME (via le role
-- applicatif sb_app). V9 ne lui donnait que SELECT sur boutiques ; on ajoute
-- INSERT/UPDATE. (La sequence d'identite de boutiques est deja couverte par le
-- GRANT ON ALL SEQUENCES de V9. La creation de l'admin initial ecrit dans users,
-- deja accessible a sb_app.)
-- =====================================================================

GRANT INSERT, UPDATE ON boutiques TO sb_app;
