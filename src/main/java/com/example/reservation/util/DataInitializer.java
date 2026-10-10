package com.example.reservation.util;

import com.example.reservation.model.*;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import java.time.LocalDateTime;

/**
 * Classe utilitaire pour initialiser un jeu de données réaliste dans la base.
 * Crée des utilisateurs, des salles et des réservations pour simuler
 * un environnement de production.
 */
public class DataInitializer {

    private final EntityManager em;

    public DataInitializer(EntityManager em) {
        this.em = em;
    }

    /**
     * Initialise les données de test dans la base de données.
     * Crée 5 utilisateurs, 5 salles et 12 réservations réalistes.
     */
    public void initData() {
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            // ========== Création des utilisateurs ==========
            Utilisateur alice = new Utilisateur("Alice Dupont", "alice.dupont@example.com");
            Utilisateur bob = new Utilisateur("Bob Martin", "bob.martin@example.com");
            Utilisateur claire = new Utilisateur("Claire Bernard", "claire.bernard@example.com");
            Utilisateur david = new Utilisateur("David Leroy", "david.leroy@example.com");
            Utilisateur emma = new Utilisateur("Emma Petit", "emma.petit@example.com");

            em.persist(alice);
            em.persist(bob);
            em.persist(claire);
            em.persist(david);
            em.persist(emma);

            // ========== Création des salles ==========
            Salle salleA = new Salle("Salle Einstein", 20, "Bâtiment A - 1er étage",
                    "Vidéoprojecteur, Tableau blanc, Wi-Fi");
            Salle salleB = new Salle("Salle Curie", 10, "Bâtiment A - 2ème étage",
                    "Écran TV, Visioconférence, Wi-Fi");
            Salle salleC = new Salle("Salle Pasteur", 50, "Bâtiment B - RDC",
                    "Vidéoprojecteur, Système audio, Micro, Wi-Fi");
            Salle salleD = new Salle("Salle Turing", 8, "Bâtiment C - 3ème étage",
                    "Tableau blanc, Wi-Fi, Prises réseau");
            Salle salleE = new Salle("Salle Ada Lovelace", 30, "Bâtiment B - 1er étage",
                    "Vidéoprojecteur, Visioconférence, Tableau interactif, Wi-Fi");

            em.persist(salleA);
            em.persist(salleB);
            em.persist(salleC);
            em.persist(salleD);
            em.persist(salleE);

            // ========== Création des réservations ==========
            LocalDateTime now = LocalDateTime.now();

            // Réservation 1 : Alice réserve la salle Einstein demain matin
            Reservation res1 = new Reservation(alice, salleA,
                    now.plusDays(1).withHour(9).withMinute(0),
                    now.plusDays(1).withHour(11).withMinute(0),
                    "CONFIRMEE", "Réunion d'équipe projet Alpha");
            em.persist(res1);

            // Réservation 2 : Bob réserve la salle Curie demain après-midi
            Reservation res2 = new Reservation(bob, salleB,
                    now.plusDays(1).withHour(14).withMinute(0),
                    now.plusDays(1).withHour(16).withMinute(0),
                    "CONFIRMEE", "Entretien client visioconférence");
            em.persist(res2);

            // Réservation 3 : Claire réserve la salle Pasteur dans 2 jours
            Reservation res3 = new Reservation(claire, salleC,
                    now.plusDays(2).withHour(10).withMinute(0),
                    now.plusDays(2).withHour(12).withMinute(0),
                    "CONFIRMEE", "Présentation trimestrielle des résultats");
            em.persist(res3);

            // Réservation 4 : David réserve la salle Turing demain
            Reservation res4 = new Reservation(david, salleD,
                    now.plusDays(1).withHour(10).withMinute(0),
                    now.plusDays(1).withHour(11).withMinute(30),
                    "EN_ATTENTE", "Session de brainstorming technique");
            em.persist(res4);

            // Réservation 5 : Emma réserve la salle Ada Lovelace dans 3 jours
            Reservation res5 = new Reservation(emma, salleE,
                    now.plusDays(3).withHour(9).withMinute(0),
                    now.plusDays(3).withHour(12).withMinute(0),
                    "CONFIRMEE", "Formation JPA/Hibernate pour les juniors");
            em.persist(res5);

            // Réservation 6 : Alice réserve la salle Curie dans 2 jours
            Reservation res6 = new Reservation(alice, salleB,
                    now.plusDays(2).withHour(9).withMinute(0),
                    now.plusDays(2).withHour(10).withMinute(0),
                    "CONFIRMEE", "Point rapide avec le manager");
            em.persist(res6);

            // Réservation 7 : Bob réserve la salle Pasteur dans 4 jours
            Reservation res7 = new Reservation(bob, salleC,
                    now.plusDays(4).withHour(14).withMinute(0),
                    now.plusDays(4).withHour(17).withMinute(0),
                    "EN_ATTENTE", "Séminaire innovation");
            em.persist(res7);

            // Réservation 8 : Claire réserve la salle Einstein dans 3 jours
            Reservation res8 = new Reservation(claire, salleA,
                    now.plusDays(3).withHour(14).withMinute(0),
                    now.plusDays(3).withHour(15).withMinute(30),
                    "CONFIRMEE", "Revue de code sprint 12");
            em.persist(res8);

            // Réservation 9 : David réserve la salle Ada Lovelace demain
            Reservation res9 = new Reservation(david, salleE,
                    now.plusDays(1).withHour(13).withMinute(0),
                    now.plusDays(1).withHour(15).withMinute(0),
                    "ANNULEE", "Workshop annulé - Design Thinking");
            em.persist(res9);

            // Réservation 10 : Emma réserve la salle Turing dans 5 jours
            Reservation res10 = new Reservation(emma, salleD,
                    now.plusDays(5).withHour(9).withMinute(0),
                    now.plusDays(5).withHour(10).withMinute(0),
                    "EN_ATTENTE", "Planification sprint 13");
            em.persist(res10);

            // Réservation 11 : Alice réserve la salle Pasteur dans 5 jours
            Reservation res11 = new Reservation(alice, salleC,
                    now.plusDays(5).withHour(10).withMinute(0),
                    now.plusDays(5).withHour(12).withMinute(0),
                    "CONFIRMEE", "Assemblée générale département IT");
            em.persist(res11);

            // Réservation 12 : Bob réserve la salle Einstein dans 6 jours
            Reservation res12 = new Reservation(bob, salleA,
                    now.plusDays(6).withHour(9).withMinute(0),
                    now.plusDays(6).withHour(11).withMinute(0),
                    "CONFIRMEE", "Démonstration nouveau produit");
            em.persist(res12);

            tx.commit();
            System.out.println("=== Données de test initialisées avec succès ===");
            System.out.println("  - 5 utilisateurs créés");
            System.out.println("  - 5 salles créées");
            System.out.println("  - 12 réservations créées");

        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            System.err.println("Erreur lors de l'initialisation des données : " + e.getMessage());
            throw new RuntimeException("Échec de l'initialisation des données", e);
        }
    }
}
