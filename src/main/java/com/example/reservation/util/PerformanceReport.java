package com.example.reservation.util;

import org.hibernate.Session;
import org.hibernate.stat.Statistics;

import javax.persistence.EntityManager;
import java.util.logging.Logger;

/**
 * Étape 7 : Rapport de performance Hibernate.
 * Mesure et analyse les performances d'accès aux données, l'efficacité
 * du cache de second niveau et du cache de requêtes.
 */
public class PerformanceReport {

    private static final Logger LOGGER = Logger.getLogger(PerformanceReport.class.getName());

    private final EntityManager em;

    public PerformanceReport(EntityManager em) {
        this.em = em;
    }

    /**
     * Affiche le rapport détaillé des statistiques Hibernate.
     */
    public void printReport() {
        Session session = em.unwrap(Session.class);
        Statistics stats = session.getSessionFactory().getStatistics();

        StringBuilder sb = new StringBuilder();
        sb.append("\n=======================================================\n");
        sb.append("         RAPPORT DE PERFORMANCE HIBERNATE              \n");
        sb.append("=======================================================\n");
        sb.append(String.format("  Sessions ouvertes              : %d%n", stats.getSessionOpenCount()));
        sb.append(String.format("  Sessions fermées               : %d%n", stats.getSessionCloseCount()));
        sb.append(String.format("  Transactions totales           : %d%n", stats.getTransactionCount()));
        sb.append(String.format("  Transactions réussies          : %d%n", stats.getSuccessfulTransactionCount()));
        sb.append("-------------------------------------------------------\n");
        sb.append("  [STATISTIQUES DES ENTITÉS]\n");
        sb.append(String.format("  Entités chargées (loads)       : %d%n", stats.getEntityLoadCount()));
        sb.append(String.format("  Entités insérées (inserts)     : %d%n", stats.getEntityInsertCount()));
        sb.append(String.format("  Entités mises à jour (updates) : %d%n", stats.getEntityUpdateCount()));
        sb.append(String.format("  Entités supprimées (deletes)   : %d%n", stats.getEntityDeleteCount()));
        sb.append(String.format("  Collections chargées           : %d%n", stats.getCollectionLoadCount()));
        sb.append("-------------------------------------------------------\n");
        sb.append("  [CACHE DE SECOND NIVEAU (L2)]\n");
        sb.append(String.format("  Cache L2 Hits                  : %d%n", stats.getSecondLevelCacheHitCount()));
        sb.append(String.format("  Cache L2 Misses                : %d%n", stats.getSecondLevelCacheMissCount()));
        sb.append(String.format("  Cache L2 Mises à jour (Puts)   : %d%n", stats.getSecondLevelCachePutCount()));
        long totalL2 = stats.getSecondLevelCacheHitCount() + stats.getSecondLevelCacheMissCount();
        double l2HitRatio = totalL2 > 0
                ? (double) stats.getSecondLevelCacheHitCount() / totalL2 * 100.0
                : 0.0;
        sb.append(String.format("  Ratio d'efficacité L2          : %.2f %%%n", l2HitRatio));
        sb.append("-------------------------------------------------------\n");
        sb.append("  [CACHE DE REQUÊTES (QUERY CACHE)]\n");
        sb.append(String.format("  Requêtes exécutées             : %d%n", stats.getQueryExecutionCount()));
        sb.append(String.format("  Query Cache Hits               : %d%n", stats.getQueryCacheHitCount()));
        sb.append(String.format("  Query Cache Misses             : %d%n", stats.getQueryCacheMissCount()));
        sb.append(String.format("  Query Cache Puts               : %d%n", stats.getQueryCachePutCount()));
        sb.append(String.format("  Durée max requête (ms)         : %d ms%n", stats.getQueryExecutionMaxTime()));
        sb.append(String.format("  Requête la plus lente          : %s%n",
                stats.getQueryExecutionMaxTimeQueryString() != null ? stats.getQueryExecutionMaxTimeQueryString() : "Aucune"));
        sb.append("=======================================================\n");

        System.out.println(sb.toString());
        LOGGER.info("Rapport de performance généré avec succès.");
    }
}
