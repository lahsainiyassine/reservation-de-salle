package com.example.reservation;

import com.example.reservation.model.Reservation;
import com.example.reservation.model.Salle;
import com.example.reservation.model.Utilisateur;
import com.example.reservation.service.ReservationService;
import com.example.reservation.util.DataInitializer;
import com.example.reservation.util.MigrationExecutor;
import com.example.reservation.util.PerformanceReport;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;

/**
 * Étape 8 : Classe principale pour exécuter l'application complète.
 * Orchestre l'exécution de la migration, l'initialisation des données,
 * la démonstration des fonctionnalités métier (JPQL, Criteria API, optimistic locking),
 * et la génération du rapport de performance Hibernate.
 */
public class MainApp {

    private static final Logger LOGGER = Logger.getLogger(MainApp.class.getName());

    public static void main(String[] args) {
        System.out.println("================================================================");
        System.out.println("   APPLICATION RÉSERVATION DE SALLES - JPA & HIBERNATE (CAPSTONE)");
        System.out.println("================================================================\n");

        EntityManagerFactory emf = null;
        EntityManager em = null;

        try {
            // 1. Initialisation de l'EntityManagerFactory
            LOGGER.info("Initialisation de l'EntityManagerFactory...");
            emf = Persistence.createEntityManagerFactory("ReservationPU");

            // 2. Étape 6 : Exécution de la migration SQL via JDBC
            System.out.println("\n>>> [ÉTAPE 6] Exécution du script de migration SQL...");
            MigrationExecutor migrationExecutor = new MigrationExecutor(
                    "jdbc:h2:mem:reservationdb;DB_CLOSE_DELAY=-1",
                    "sa",
                    ""
            );
            migrationExecutor.executeMigration("db/migration/v2_0_add_description.sql");
            System.out.println("Script 'v2_0_add_description.sql' appliqué avec succès !");

            // 3. Étape 3 : Initialisation du jeu de données réaliste
            System.out.println("\n>>> [ÉTAPE 3] Insertion du jeu de données de test...");
            em = emf.createEntityManager();
            DataInitializer initializer = new DataInitializer(em);
            initializer.initData();

            // 4. Démonstration du service métier
            ReservationService reservationService = new ReservationService(em);

            // 5. Étape 9 : Requête JPQL
            System.out.println("\n>>> [ÉTAPE 9.1] Requête JPQL : Réservations de la 'Salle Einstein'...");
            List<Reservation> jpqlResults = reservationService.findReservationsBySalleAndDateJPQL(
                    "Salle Einstein",
                    LocalDateTime.now()
            );
            System.out.println("Nombre de réservations trouvées via JPQL : " + jpqlResults.size());
            for (Reservation r : jpqlResults) {
                System.out.printf("  - ID: %d | Objet: %s | Utilisateur: %s | Début: %s | Statut: %s%n",
                        r.getId(), r.getObjet(), r.getUtilisateur().getNom(), r.getDateDebut(), r.getStatut());
            }

            // 6. Étape 9 : Requête Criteria API
            System.out.println("\n>>> [ÉTAPE 9.2] Requête Criteria API : Réservations de la 'Salle Einstein'...");
            List<Reservation> criteriaResults = reservationService.findReservationsBySalleAndDateCriteria(
                    "Salle Einstein",
                    LocalDateTime.now()
            );
            System.out.println("Nombre de réservations trouvées via Criteria API : " + criteriaResults.size());

            // 7. Étape 10 : Démonstration de création et détection de conflit
            System.out.println("\n>>> [ÉTAPE 10] Démonstration gestion des transactions & conflits...");
            Salle salleCurie = em.createQuery("SELECT s FROM Salle s WHERE s.nom = 'Salle Curie'", Salle.class)
                    .getSingleResult();
            Utilisateur alice = em.createQuery("SELECT u FROM Utilisateur u WHERE u.email = 'alice.dupont@example.com'", Utilisateur.class)
                    .getSingleResult();

            LocalDateTime creneauDebut = LocalDateTime.now().plusDays(10).withHour(14).withMinute(0);
            LocalDateTime creneauFin = LocalDateTime.now().plusDays(10).withHour(16).withMinute(0);

            Reservation nouvelleRes = new Reservation(alice, salleCurie, creneauDebut, creneauFin,
                    "CONFIRMEE", "Atelier Architecture Logicielle");
            reservationService.createReservation(nouvelleRes);
            System.out.println("Nouvelle réservation créée avec succès, ID = " + nouvelleRes.getId());

            // Test de tentative de conflit
            System.out.println("Tentative de réservation concurrente sur le même créneau...");
            try {
                Reservation conflitRes = new Reservation(alice, salleCurie,
                        creneauDebut.plusMinutes(30), creneauFin.plusMinutes(30),
                        "EN_ATTENTE", "Conflit volontaire");
                reservationService.createReservation(conflitRes);
            } catch (IllegalStateException e) {
                System.out.println("SUCCÈS : Conflit correctement détecté et bloqué ! Message : " + e.getMessage());
            }

            // 8. Étape 2 : Test d'accès au cache de second niveau
            System.out.println("\n>>> [ÉTAPE 2] Test du cache de second niveau (L2)...");
            Long salleId = salleCurie.getId();
            em.close(); // Fermer l'EntityManager pour vider le cache L1

            EntityManager em2 = emf.createEntityManager();
            Salle salleFromL2 = em2.find(Salle.class, salleId); // Chargé depuis le cache L2
            System.out.println("Salle rechargée depuis le cache L2 : " + salleFromL2.getNom());

            // 9. Étape 7 : Génération et affichage du rapport de performance
            System.out.println("\n>>> [ÉTAPE 7] Génération du rapport de performance Hibernate...");
            PerformanceReport report = new PerformanceReport(em2);
            report.printReport();

            em2.close();

            System.out.println("=== EXÉCUTION TERMINÉE AVEC SUCCÈS ===");

        } catch (Exception e) {
            LOGGER.severe("Erreur inattendue dans l'application principale : " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
            if (emf != null && emf.isOpen()) {
                emf.close();
            }
        }
    }
}
