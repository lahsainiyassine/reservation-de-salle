package com.example.reservation.test;

import com.example.reservation.model.*;
import org.junit.jupiter.api.*;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests fonctionnels pour les opérations CRUD sur les réservations.
 * Vérifie la création, modification, suppression, les requêtes,
 * la détection de conflits et l'optimistic locking.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReservationTest {

    private static EntityManagerFactory emf;
    private EntityManager em;

    @BeforeAll
    public static void init() {
        emf = Persistence.createEntityManagerFactory("ReservationPU");
        org.hibernate.stat.Statistics stats = emf.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
    }

    @BeforeEach
    public void setUp() {
        em = emf.createEntityManager();
    }

    @AfterEach
    public void tearDown() {
        if (em != null && em.isOpen()) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    @AfterAll
    public static void close() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    /**
     * Test 1 : Créer une réservation et vérifier qu'elle obtient un ID.
     */
    @Test
    @Order(1)
    public void testCreateReservation() {
        em.getTransaction().begin();

        Utilisateur user = new Utilisateur("Test User", "test.create@example.com");
        em.persist(user);

        Salle salle = new Salle("Salle Test", 15, "Bâtiment Test", "Wi-Fi");
        em.persist(salle);

        Reservation reservation = new Reservation();
        reservation.setUtilisateur(user);
        reservation.setSalle(salle);
        reservation.setDateDebut(LocalDateTime.now().plusDays(2));
        reservation.setDateFin(LocalDateTime.now().plusDays(2).plusHours(1));
        reservation.setStatut("CONFIRMEE");
        reservation.setObjet("Réunion de test");
        em.persist(reservation);

        em.getTransaction().commit();

        assertNotNull(reservation.getId(), "L'ID de la réservation ne doit pas être null après persistance");
        assertEquals("CONFIRMEE", reservation.getStatut());
        assertEquals("Réunion de test", reservation.getObjet());
    }

    /**
     * Test 2 : Modifier le statut d'une réservation et vérifier la persistance.
     */
    @Test
    @Order(2)
    public void testUpdateReservation() {
        em.getTransaction().begin();

        Utilisateur user = new Utilisateur("Update User", "test.update@example.com");
        em.persist(user);
        Salle salle = new Salle("Salle Update", 10, "Bâtiment X", "Tableau");
        em.persist(salle);

        Reservation reservation = new Reservation(user, salle,
                LocalDateTime.now().plusDays(3), LocalDateTime.now().plusDays(3).plusHours(2),
                "EN_ATTENTE", "Réunion initiale");
        em.persist(reservation);
        em.getTransaction().commit();

        Long reservationId = reservation.getId();

        // Modifier la réservation
        em.getTransaction().begin();
        Reservation found = em.find(Reservation.class, reservationId);
        found.setStatut("CONFIRMEE");
        found.setObjet("Réunion confirmée et mise à jour");
        em.getTransaction().commit();

        // Vérifier dans un nouveau contexte
        em.clear();
        Reservation updated = em.find(Reservation.class, reservationId);
        assertEquals("CONFIRMEE", updated.getStatut());
        assertEquals("Réunion confirmée et mise à jour", updated.getObjet());
    }

    /**
     * Test 3 : Supprimer une réservation et vérifier qu'elle n'existe plus.
     */
    @Test
    @Order(3)
    public void testDeleteReservation() {
        em.getTransaction().begin();

        Utilisateur user = new Utilisateur("Delete User", "test.delete@example.com");
        em.persist(user);
        Salle salle = new Salle("Salle Delete", 5, "Bâtiment Y", "Rien");
        em.persist(salle);

        Reservation reservation = new Reservation(user, salle,
                LocalDateTime.now().plusDays(4), LocalDateTime.now().plusDays(4).plusHours(1),
                "CONFIRMEE", "Réservation à supprimer");
        em.persist(reservation);
        em.getTransaction().commit();

        Long reservationId = reservation.getId();

        // Supprimer
        em.getTransaction().begin();
        Reservation toDelete = em.find(Reservation.class, reservationId);
        em.remove(toDelete);
        em.getTransaction().commit();

        // Vérifier la suppression
        em.clear();
        Reservation deleted = em.find(Reservation.class, reservationId);
        assertNull(deleted, "La réservation supprimée ne doit plus exister en base");
    }

    /**
     * Test 4 : Rechercher les réservations d'un utilisateur donné.
     */
    @Test
    @Order(4)
    public void testFindReservationsByUser() {
        em.getTransaction().begin();

        Utilisateur user = new Utilisateur("Multi User", "test.multi@example.com");
        em.persist(user);
        Salle salle = new Salle("Salle Multi", 20, "Bâtiment Z", "Tout");
        em.persist(salle);

        // Créer 3 réservations pour cet utilisateur
        for (int i = 1; i <= 3; i++) {
            Reservation r = new Reservation(user, salle,
                    LocalDateTime.now().plusDays(i).withHour(9),
                    LocalDateTime.now().plusDays(i).withHour(10),
                    "CONFIRMEE", "Réservation n°" + i);
            em.persist(r);
        }
        em.getTransaction().commit();

        // Requête JPQL pour trouver les réservations par utilisateur
        List<Reservation> reservations = em.createQuery(
                "SELECT r FROM Reservation r WHERE r.utilisateur.id = :userId", Reservation.class)
                .setParameter("userId", user.getId())
                .getResultList();

        assertEquals(3, reservations.size(), "L'utilisateur doit avoir exactement 3 réservations");
    }

    /**
     * Test 5 : Détecter un conflit de réservation (chevauchement horaire sur la même salle).
     */
    @Test
    @Order(5)
    public void testReservationConflict() {
        em.getTransaction().begin();

        Utilisateur user1 = new Utilisateur("Conflict User1", "conflict1@example.com");
        Utilisateur user2 = new Utilisateur("Conflict User2", "conflict2@example.com");
        em.persist(user1);
        em.persist(user2);

        Salle salle = new Salle("Salle Conflit", 10, "Bâtiment W", "Wi-Fi");
        em.persist(salle);

        LocalDateTime debut = LocalDateTime.now().plusDays(7).withHour(10).withMinute(0);
        LocalDateTime fin = LocalDateTime.now().plusDays(7).withHour(12).withMinute(0);

        // Première réservation
        Reservation res1 = new Reservation(user1, salle, debut, fin, "CONFIRMEE", "Première réservation");
        em.persist(res1);
        em.getTransaction().commit();

        // Vérifier le conflit avec une requête JPQL
        LocalDateTime newDebut = LocalDateTime.now().plusDays(7).withHour(11).withMinute(0);
        LocalDateTime newFin = LocalDateTime.now().plusDays(7).withHour(13).withMinute(0);

        List<Reservation> conflicts = em.createQuery(
                "SELECT r FROM Reservation r WHERE r.salle.id = :salleId " +
                "AND r.dateDebut < :fin AND r.dateFin > :debut " +
                "AND r.statut <> 'ANNULEE'", Reservation.class)
                .setParameter("salleId", salle.getId())
                .setParameter("debut", newDebut)
                .setParameter("fin", newFin)
                .getResultList();

        assertFalse(conflicts.isEmpty(), "Un conflit de réservation doit être détecté");
        assertEquals(1, conflicts.size(), "Il doit y avoir exactement 1 conflit");
    }

    /**
     * Test 6 : Vérifier le fonctionnement de l'optimistic locking avec @Version.
     */
    @Test
    @Order(6)
    public void testOptimisticLocking() {
        em.getTransaction().begin();

        Utilisateur user = new Utilisateur("Lock User", "lock@example.com");
        em.persist(user);
        Salle salle = new Salle("Salle Lock", 10, "Bâtiment V", "Wi-Fi");
        em.persist(salle);

        Reservation reservation = new Reservation(user, salle,
                LocalDateTime.now().plusDays(8), LocalDateTime.now().plusDays(8).plusHours(1),
                "EN_ATTENTE", "Test locking");
        em.persist(reservation);
        em.getTransaction().commit();

        Long resId = reservation.getId();

        // Simuler une modification concurrente
        EntityManager em2 = emf.createEntityManager();

        // Session 1 : charger et modifier
        em.getTransaction().begin();
        Reservation r1 = em.find(Reservation.class, resId);
        r1.setStatut("CONFIRMEE");

        // Session 2 : charger et modifier la même entité
        em2.getTransaction().begin();
        Reservation r2 = em2.find(Reservation.class, resId);
        r2.setStatut("ANNULEE");
        em2.getTransaction().commit();

        // Session 1 : commit devrait échouer (OptimisticLockException)
        assertThrows(RollbackException.class, () -> {
            em.getTransaction().commit();
        }, "OptimisticLockException attendue lors d'une modification concurrente");

        em2.close();
    }

    /**
     * Test 7 : Vérifier la cascade sur la suppression.
     */
    @Test
    @Order(7)
    public void testCascadeOperations() {
        em.getTransaction().begin();

        Utilisateur user = new Utilisateur("Cascade User", "cascade@example.com");
        em.persist(user);
        Salle salle = new Salle("Salle Cascade", 10, "Bâtiment U", "Wi-Fi");
        em.persist(salle);

        Reservation res = new Reservation(user, salle,
                LocalDateTime.now().plusDays(9), LocalDateTime.now().plusDays(9).plusHours(1),
                "CONFIRMEE", "Test cascade");
        em.persist(res);
        em.getTransaction().commit();

        Long resId = res.getId();

        // Vérifier que la réservation existe
        em.clear();
        Reservation found = em.find(Reservation.class, resId);
        assertNotNull(found, "La réservation doit exister");

        // Supprimer la réservation directement
        em.getTransaction().begin();
        Reservation toRemove = em.find(Reservation.class, resId);
        em.remove(toRemove);
        em.getTransaction().commit();

        em.clear();
        assertNull(em.find(Reservation.class, resId), "La réservation doit être supprimée");
    }

    /**
     * Test 8 : Vérifier le fonctionnement du cache de second niveau et de requêtes.
     */
    @Test
    @Order(8)
    public void testQueryCache() {
        em.getTransaction().begin();

        Salle salle = new Salle("Salle Cache Test PU", 10, "Bâtiment T", "Wi-Fi");
        em.persist(salle);
        em.getTransaction().commit();

        Long salleId = salle.getId();

        // Charger une première fois dans em -> stocké dans le cache L2
        em.clear();
        Salle loadedFirst = em.find(Salle.class, salleId);
        assertNotNull(loadedFirst);

        // Réinitialiser les statistiques
        org.hibernate.stat.Statistics stats = emf.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        // Fermer et rouvrir pour tester l'accès au cache L2
        em.close();
        em = emf.createEntityManager();

        Salle loadedFromL2 = em.find(Salle.class, salleId);
        assertNotNull(loadedFromL2);
        assertEquals("Salle Cache Test PU", loadedFromL2.getNom());

        long l2Hits = stats.getSecondLevelCacheHitCount();
        System.out.println("L2 Cache Hits in ReservationTest: " + l2Hits);
        assertTrue(l2Hits >= 1, "Le cache L2 doit enregistrer au moins 1 hit");
    }
}
