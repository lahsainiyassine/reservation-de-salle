package com.example.reservation.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Étape 6 : Exécuteur de scripts de migration SQL via JDBC.
 * Permet d'automatiser l'application des évolutions du schéma de base de données.
 */
public class MigrationExecutor {

    private static final Logger LOGGER = Logger.getLogger(MigrationExecutor.class.getName());

    private final String jdbcUrl;
    private final String username;
    private final String password;

    public MigrationExecutor(String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    /**
     * Lit un script SQL situé dans les ressources (classpath) et l'exécute instruction par instruction.
     *
     * @param scriptPath Chemin relatif vers le fichier SQL dans les ressources (ex: "db/migration/v2_0_add_description.sql")
     * @throws Exception En cas d'erreur de lecture ou d'exécution SQL
     */
    public void executeMigration(String scriptPath) throws Exception {
        LOGGER.info("Début de l'exécution de la migration : " + scriptPath);

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             InputStream is = getClass().getClassLoader().getResourceAsStream(scriptPath)) {

            if (is == null) {
                String errorMsg = "Fichier de migration introuvable dans le classpath : " + scriptPath;
                LOGGER.severe(errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sql = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    // Ignorer les commentaires et lignes vides
                    String trimmed = line.trim();
                    if (!trimmed.startsWith("--") && !trimmed.isEmpty()) {
                        sql.append(line).append(" ");
                    }
                }

                // Découpage par point-virgule pour exécuter chaque instruction individuellement
                String[] statements = sql.toString().split(";");
                try (Statement stmt = conn.createStatement()) {
                    for (String singleSql : statements) {
                        String execSql = singleSql.trim();
                        if (!execSql.isEmpty()) {
                            LOGGER.fine("Exécution SQL : " + execSql);
                            stmt.execute(execSql);
                        }
                    }
                }
            }

            LOGGER.info("Migration exécutée avec succès : " + scriptPath);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'exécution de la migration : " + scriptPath, e);
            throw e;
        }
    }
}
