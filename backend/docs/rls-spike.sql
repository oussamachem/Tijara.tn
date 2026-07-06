-- =====================================================================
-- PHASE 0 — Spike RLS (isolation multi-tenant) sur une table jouet `demo`.
-- Objectif : PROUVER le mecanisme d'isolation avant de le generaliser (Phase 1).
--
-- Points durs demontres ici :
--   * FORCE ROW LEVEL SECURITY : sinon le PROPRIETAIRE de la table (et tout
--     SUPERUSER) IGNORE la RLS -> la policy serait un decor.
--   * Connexion runtime via un ROLE APPLICATIF NON-SUPERUSER : un superuser
--     bypasse TOUJOURS la RLS. L'app ne doit donc jamais tourner en superuser.
--   * GUC de session `app.current_boutique` pose par transaction (SET LOCAL en
--     prod ; SET simple dans le spike). `current_setting(.., true)` => NULL si
--     non pose => policy fausse => 0 ligne (fail-closed, sur).
--   * WITH CHECK : empeche d'INSERER/UPDATER une ligne dans le tenant d'autrui.
--   * SUPER_ADMIN plateforme = role BYPASSRLS (ou superuser) : voit tout.
-- =====================================================================

-- --- Setup (execute par un SUPERUSER / proprietaire) ---
DROP TABLE IF EXISTS demo;
CREATE TABLE demo (
    id          BIGSERIAL PRIMARY KEY,
    boutique_id BIGINT NOT NULL,
    val         TEXT
);

ALTER TABLE demo ENABLE ROW LEVEL SECURITY;
ALTER TABLE demo FORCE  ROW LEVEL SECURITY;   -- s'applique AUSSI au proprietaire

-- Une seule policy : lecture ET ecriture limitees au tenant courant.
-- NULLIF(..,'') est CRUCIAL : un GUC custom, une fois RESET (ou apres un SET LOCAL
-- rendu au pool), revient a la chaine VIDE '' et non a NULL -> ''::bigint leve
-- "invalid input syntax for type bigint". NULLIF ramene unset ET reset a NULL
-- -> comparaison NULL -> aucune ligne (fail-closed).
CREATE POLICY tenant_isolation ON demo
    USING      (boutique_id = NULLIF(current_setting('app.current_boutique', true), '')::bigint)
    WITH CHECK (boutique_id = NULLIF(current_setting('app.current_boutique', true), '')::bigint);

-- Role applicatif NON-SUPERUSER pour les connexions runtime.
DROP ROLE IF EXISTS sb_rls_spike;
CREATE ROLE sb_rls_spike LOGIN PASSWORD 'spike_pwd' NOSUPERUSER;
GRANT SELECT, INSERT, UPDATE, DELETE ON demo TO sb_rls_spike;
GRANT USAGE, SELECT ON SEQUENCE demo_id_seq TO sb_rls_spike;

-- Donnees de 2 tenants (insert par le superuser, qui bypasse la RLS).
INSERT INTO demo (boutique_id, val) VALUES (1, 'A-1'), (1, 'A-2'), (2, 'B-1');

-- --- Preuves (connexion en tant que sb_rls_spike, NON-superuser) ---

-- Tenant 1 : ne voit que ses 2 lignes.
SET app.current_boutique = '1';
SELECT count(*) FROM demo;                       -- attendu : 2

-- Tenant 2 : ne voit que sa 1 ligne.
SET app.current_boutique = '2';
SELECT count(*) FROM demo;                       -- attendu : 1

-- Isolation en ECRITURE : tenant 1 ne peut pas modifier une ligne de 2.
SET app.current_boutique = '1';
UPDATE demo SET val = 'hack' WHERE boutique_id = 2;   -- attendu : 0 ligne modifiee

-- WITH CHECK : tenant 1 ne peut pas INSERER chez le tenant 2.
INSERT INTO demo (boutique_id, val) VALUES (2, 'evil');   -- attendu : ERREUR (violation policy)

-- Fail-closed : sans GUC, aucune ligne.
RESET app.current_boutique;
SELECT count(*) FROM demo;                       -- attendu : 0

-- --- Bypass SUPER_ADMIN (superuser / BYPASSRLS) : voit tout ---
-- (connexion superuser) SELECT count(*) FROM demo;   -- attendu : total tous tenants

-- --- Teardown ---
-- DROP TABLE demo;  REVOKE ...;  DROP ROLE sb_rls_spike;
