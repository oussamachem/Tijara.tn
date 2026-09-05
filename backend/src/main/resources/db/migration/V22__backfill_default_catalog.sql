-- =====================================================================
-- V22 — Rétro-remplissage : dote les boutiques EXISTANTES du catalogue par défaut
-- (10 couleurs + 20 tailles), comme les nouvelles boutiques (cf. BoutiqueService.seedDefaultCatalog).
-- IDEMPOTENT : n'ajoute que ce qui manque (ON CONFLICT sur l'unicité par boutique) ; ne modifie
-- ni ne supprime l'existant. Exécuté par le PROPRIÉTAIRE (Flyway) -> RLS contournée, boutique_id explicite.
-- =====================================================================

-- Couleurs par défaut manquantes (par nom, insensible à l'ordre).
INSERT INTO colors (boutique_id, name, hex)
SELECT b.id, d.name, d.hex
FROM boutiques b
CROSS JOIN (VALUES
    ('Noir', '#111111'), ('Blanc', '#FFFFFF'), ('Gris', '#9CA3AF'), ('Rouge', '#DC2626'),
    ('Bleu', '#2563EB'), ('Vert', '#16A34A'), ('Jaune', '#FACC15'), ('Rose', '#EC4899'),
    ('Beige', '#D9C6A5'), ('Marron', '#92400E')
) AS d(name, hex)
ON CONFLICT ON CONSTRAINT uk_colors_boutique_name DO NOTHING;

-- Tailles par défaut manquantes (ajoutées APRÈS les tailles existantes de la boutique).
INSERT INTO sizes (boutique_id, label, position)
SELECT b.id, d.label,
       COALESCE((SELECT MAX(s2.position) FROM sizes s2 WHERE s2.boutique_id = b.id), 0) + d.ord
FROM boutiques b
CROSS JOIN (VALUES
    ('S', 1), ('M', 2), ('L', 3), ('XL', 4), ('XXL', 5), ('XXXL', 6),
    ('36', 7), ('37', 8), ('38', 9), ('39', 10), ('40', 11), ('41', 12), ('42', 13),
    ('43', 14), ('44', 15), ('45', 16), ('46', 17), ('47', 18), ('48', 19), ('49', 20)
) AS d(label, ord)
ON CONFLICT ON CONSTRAINT uk_sizes_boutique_label DO NOTHING;
