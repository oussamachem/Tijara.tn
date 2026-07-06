-- =====================================================================
-- Multi-tenancy Phase 2 — le role SUPER_ADMIN (plateforme) rejoint les roles
-- autorises. La contrainte ck_users_role de V1 ne listait que ADMIN/VENDEUR.
-- (CLIENT sera ajoute en Phase 4 avec le marketplace.)
-- =====================================================================

ALTER TABLE users DROP CONSTRAINT ck_users_role;
ALTER TABLE users ADD  CONSTRAINT ck_users_role
    CHECK (role IN ('SUPER_ADMIN', 'ADMIN', 'VENDEUR'));
