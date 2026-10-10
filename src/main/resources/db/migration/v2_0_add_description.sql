-- ====================================================================
-- Script de migration V2.0 : Application Réservation de Salles
-- Compatible dialecte H2 / SQL standard
-- ====================================================================

-- 1. Ajout de la colonne 'description' à la table 'salles' pour enrichir les informations
ALTER TABLE salles ADD COLUMN IF NOT EXISTS description VARCHAR(255);

-- 2. Ajout d'un index unique sur la colonne 'email' de la table 'utilisateurs' pour accélérer les recherches et garantir l'unicité
CREATE UNIQUE INDEX IF NOT EXISTS idx_utilisateur_email ON utilisateurs(email);

-- 3. Ajout d'un index sur les dates de réservations pour optimiser les recherches de conflits
CREATE INDEX IF NOT EXISTS idx_reservations_dates ON reservations(date_debut, date_fin);
