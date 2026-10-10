package com.example.reservation.test;

import com.example.reservation.model.Salle;
import com.example.reservation.model.Utilisateur;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.*;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires validant le comportement du cache de second niveau (L2)
 * et du cache de requêtes Hibernate/Ehcache.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CacheTest {

    private static EntityManagerFactory emf;
    private static Statistics stats;

    @BeforeAll
    public static void init() {
        emf = Persistence.createEntityManagerFactory("ReservationPU");
        stats = emf.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
    }

    @AfterAll
    public static void close() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    @BeforeEach
    public void resetStats() {
        stats.clear();
    }

    /**
     * Test 1 : Vérifier qu'une entité annotée @Cacheable est servie depuis le cache L2
     * lors d'un accès ultérieur dans une nouvelle session / EntityManager.
     */
    @Test
    @Order(1)
    public void testSecondLevelCacheHit() {
        // 1. Session 1 : Créer et persister l'entité
        EntityManager em1 = emf.createEntityManager();
        em1.getTransaction().begin();
        Salle salle = new Salle("Salle Cache Test", 12, "Étage 1", "Vidéo");
        em1.persist(salle);
        em1.getTransaction().commit();
        Long salleId = salle.getId();
        em1.close();

        // 2. Session 2 : Premier chargement -> peuple le cache L2
        EntityManager em2 = emf.createEntityManager();
        Salle loadedInEm2 = em2.find(Salle.class, salleId);
        assertNotNull(loadedInEm2);
        em2.close();

        stats.clear();

        // 3. Session 3 : Deuxième chargement -> doit être un hit sur le cache L2
        EntityManager em3 = emf.createEntityManager();
        Salle salleFromL2 = em3.find(Salle.class, salleId);

        assertNotNull(salleFromL2);
        assertEquals("Salle Cache Test", salleFromL2.getNom());

        long l2Hits = stats.getSecondLevelCacheHitCount();
        System.out.println("Second Level Cache Hits: " + l2Hits);
        assertTrue(l2Hits >= 1, "Le cache L2 doit enregistrer au moins 1 hit pour Salle");

        em3.close();
    }

    /**
     * Test 2 : Vérifier que l'éviction du cache force un rechargement en base de données.
     */
    @Test
    @Order(2)
    public void testCacheEviction() {
        EntityManager em1 = emf.createEntityManager();
        em1.getTransaction().begin();
        Utilisateur user = new Utilisateur("User Evict", "evict@example.com");
        em1.persist(user);
        em1.getTransaction().commit();
        Long userId = user.getId();
        em1.close();

        // Charger une fois pour s'assurer de la présence en cache L2
        EntityManager emLoad = emf.createEntityManager();
        emLoad.find(Utilisateur.class, userId);
        emLoad.close();

        // Éviction manuelle de l'entité du cache L2 via le Cache JPA
        emf.getCache().evict(Utilisateur.class, userId);
        assertFalse(emf.getCache().contains(Utilisateur.class, userId), "L'entité ne doit plus être dans le cache L2");

        stats.clear();

        // Rechargement : doit être un L2 miss
        EntityManager em2 = emf.createEntityManager();
        Utilisateur reloaded = em2.find(Utilisateur.class, userId);
        assertNotNull(reloaded);

        long l2Misses = stats.getSecondLevelCacheMissCount();
        System.out.println("Second Level Cache Misses: " + l2Misses);
        assertTrue(l2Misses >= 1, "Le rechargement après éviction doit produire un miss sur le cache L2");

        em2.close();
    }

    /**
     * Test 3 : Vérifier le cache de requêtes (Query Cache).
     */
    @Test
    @Order(3)
    public void testQueryCache() {
        EntityManager em1 = emf.createEntityManager();
        em1.getTransaction().begin();

        Salle salle1 = new Salle("Salle QCache 1", 20, "Bat A", "Projecteur");
        Salle salle2 = new Salle("Salle QCache 2", 30, "Bat B", "Ecran");
        em1.persist(salle1);
        em1.persist(salle2);
        em1.getTransaction().commit();
        em1.close();

        stats.clear();

        EntityManager em2 = emf.createEntityManager();
        // Première exécution
        List<Salle> result1 = em2.createQuery("SELECT s FROM Salle s WHERE s.capacite >= :cap", Salle.class)
                .setParameter("cap", 20)
                .setHint("org.hibernate.cacheable", true)
                .getResultList();
        assertFalse(result1.isEmpty());
        em2.close();

        // Deuxième exécution dans une nouvelle session
        EntityManager em3 = emf.createEntityManager();
        List<Salle> result2 = em3.createQuery("SELECT s FROM Salle s WHERE s.capacite >= :cap", Salle.class)
                .setParameter("cap", 20)
                .setHint("org.hibernate.cacheable", true)
                .getResultList();
        assertEquals(result1.size(), result2.size());
        em3.close();

        long queryHits = stats.getQueryCacheHitCount();
        System.out.println("Query Cache Hits: " + queryHits);
        assertTrue(queryHits >= 1, "Le cache de requêtes doit enregistrer au moins 1 hit");
    }
}
