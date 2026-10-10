package com.example.reservation.service;

import com.example.reservation.model.Reservation;
import com.example.reservation.model.Salle;
import com.example.reservation.model.Utilisateur;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.OptimisticLockException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service métier gérant les réservations de salles.
 * Couvre les étapes 9 (JPQL & Criteria API), 10 (Transactions & Isolation) et 11 (Logs & Exceptions).
 */
public class ReservationService {

    private static final Logger LOGGER = Logger.getLogger(ReservationService.class.getName());

    private final EntityManager em;

    public ReservationService(EntityManager em) {
        this.em = em;
    }

    /**
     * Étape 10 & 11 : Création d'une réservation avec gestion transactionnelle et vérification de conflit.
     */
    public Reservation createReservation(Reservation reservation) {
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            // Vérification des dates
            if (reservation.getDateDebut().isAfter(reservation.getDateFin())) {
                throw new IllegalArgumentException("La date de début doit être antérieure à la date de fin.");
            }

            // Détection de conflit de réservation
            boolean hasConflict = checkConflictInternal(
                    reservation.getSalle().getId(),
                    reservation.getDateDebut(),
                    reservation.getDateFin(),
                    null
            );

            if (hasConflict) {
                LOGGER.warning("Tentative de réservation en conflit pour la salle : " + reservation.getSalle().getNom());
                throw new IllegalStateException("La salle est déjà réservée sur ce créneau horaire.");
            }

            em.persist(reservation);
            tx.commit();

            LOGGER.info("Création de la réservation réussie (ID: " + reservation.getId() + ") pour l'utilisateur : "
                    + (reservation.getUtilisateur() != null ? reservation.getUtilisateur().getEmail() : "N/A"));

            return reservation;
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            LOGGER.severe("Erreur lors de la création de la réservation : " + e.getMessage());
            throw e;
        }
    }

    /**
     * Étape 10 : Mise à jour avec gestion de l'Optimistic Locking (@Version).
     */
    public Reservation updateReservation(Reservation reservation) {
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Reservation updated = em.merge(reservation);
            tx.commit();
            LOGGER.info("Mise à jour réussie de la réservation ID : " + updated.getId());
            return updated;
        } catch (OptimisticLockException ole) {
            if (tx.isActive()) {
                tx.rollback();
            }
            LOGGER.log(Level.WARNING, "Conflit d'accès concurrent (OptimisticLockException) pour la réservation ID "
                    + reservation.getId() + " : la réservation a été modifiée par un autre utilisateur.", ole);
            throw ole;
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            LOGGER.log(Level.SEVERE, "Erreur lors de la mise à jour de la réservation ID " + reservation.getId(), e);
            throw e;
        }
    }

    /**
     * Annule une réservation (passage du statut à ANNULEE).
     */
    public void cancelReservation(Long reservationId) {
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Reservation res = em.find(Reservation.class, reservationId);
            if (res != null) {
                res.setStatut("ANNULEE");
                tx.commit();
                LOGGER.info("Réservation ID " + reservationId + " annulée avec succès.");
            } else {
                tx.rollback();
                LOGGER.warning("Impossible d'annuler : réservation non trouvée pour l'ID " + reservationId);
            }
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            LOGGER.severe("Erreur lors de l'annulation de la réservation ID " + reservationId + " : " + e.getMessage());
            throw e;
        }
    }

    /**
     * Étape 9 : Recherche par JPQL paramétré (protection contre injections SQL).
     * Récupère les réservations pour une salle donnée après une certaine date.
     */
    public List<Reservation> findReservationsBySalleAndDateJPQL(String nomSalle, LocalDateTime date) {
        LOGGER.info("Exécution de la recherche JPQL pour la salle : " + nomSalle + " après " + date);
        String jpql = "SELECT r FROM Reservation r JOIN FETCH r.salle s JOIN FETCH r.utilisateur u " +
                      "WHERE s.nom = :nomSalle AND r.dateDebut > :date ORDER BY r.dateDebut ASC";

        return em.createQuery(jpql, Reservation.class)
                .setParameter("nomSalle", nomSalle)
                .setParameter("date", date)
                .setHint("org.hibernate.cacheable", true)
                .getResultList();
    }

    /**
     * Étape 9 : Recherche dynamique avec Criteria API (requête type-safe).
     * Construit dynamiquement la requête de filtrage par nom de salle et date.
     */
    public List<Reservation> findReservationsBySalleAndDateCriteria(String nomSalle, LocalDateTime date) {
        LOGGER.info("Exécution de la recherche Criteria API pour la salle : " + nomSalle + " après " + date);
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Reservation> cq = cb.createQuery(Reservation.class);
        Root<Reservation> root = cq.from(Reservation.class);

        // Fetch des relations LAZY pour éviter le problème N+1
        root.fetch("salle");
        root.fetch("utilisateur");

        Predicate predicate = cb.and(
                cb.equal(root.get("salle").get("nom"), nomSalle),
                cb.greaterThan(root.get("dateDebut"), date)
        );

        cq.select(root).where(predicate).orderBy(cb.asc(root.get("dateDebut")));

        return em.createQuery(cq)
                .setHint("org.hibernate.cacheable", true)
                .getResultList();
    }

    /**
     * Recherche les réservations par identifiant d'utilisateur.
     */
    public List<Reservation> findReservationsByUtilisateur(Long utilisateurId) {
        String jpql = "SELECT r FROM Reservation r WHERE r.utilisateur.id = :userId ORDER BY r.dateDebut DESC";
        return em.createQuery(jpql, Reservation.class)
                .setParameter("userId", utilisateurId)
                .getResultList();
    }

    /**
     * Vérification de chevauchement horaire (conflit de réservation).
     */
    public boolean checkConflict(Long salleId, LocalDateTime debut, LocalDateTime fin) {
        return checkConflictInternal(salleId, debut, fin, null);
    }

    private boolean checkConflictInternal(Long salleId, LocalDateTime debut, LocalDateTime fin, Long excludeReservationId) {
        String jpql = "SELECT COUNT(r) FROM Reservation r WHERE r.salle.id = :salleId " +
                      "AND r.dateDebut < :fin AND r.dateFin > :debut " +
                      "AND r.statut <> 'ANNULEE'";

        if (excludeReservationId != null) {
            jpql += " AND r.id <> :excludeId";
        }

        var query = em.createQuery(jpql, Long.class)
                .setParameter("salleId", salleId)
                .setParameter("debut", debut)
                .setParameter("fin", fin);

        if (excludeReservationId != null) {
            query.setParameter("excludeId", excludeReservationId);
        }

        Long count = query.getSingleResult();
        return count > 0;
    }
}
