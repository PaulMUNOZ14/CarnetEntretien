package com.carnetentretien.carnetentretien.db;

import com.carnetentretien.carnetentretien.service.AppPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Gestion de la connexion SQLite et du schéma de la base : création des tables
 * et migration des anciennes bases. Le fichier se trouve dans le dossier défini par {@link AppPaths}.
 */
public class DatabaseManager {
    // Le fichier de la base de données est dans le dossier de données de l'application (voir AppPaths)
    private static String url() {
        return "jdbc:sqlite:" + AppPaths.fichierBaseDeDonnees();
    }

    /**
     * Ouvre une connexion vers la base.
     * Crée au besoin le dossier de données.
     *
     * @return la connexion, ou {@code null} si elle a échoué (l'erreur est affichée dans la console)
     */
    public static Connection connect() {
        Connection conn = null;
        try {
            Files.createDirectories(AppPaths.fichierBaseDeDonnees().getParent());
            conn = DriverManager.getConnection(url());
        } catch (IOException | SQLException e) {
            System.out.println("Erreur de connexion à SQLite : " + e.getMessage());
        }
        return conn;
    }

    /**
     * Crée les tables {@code Vehicule}, {@code Entretien_Fait}, {@code Preconisation} et
     * {@code Entretien_Planifie} si elles n'existent pas, et ajoute les colonnes récentes
     * ({@code immatriculation}, {@code image_url}, {@code image_locale}) aux bases créées par une
     * version précédente. Peut être appelée plusieurs fois sans perte de données.
     */
    public static void initDatabase() {
        String sqlVehicule = "CREATE TABLE IF NOT EXISTS Vehicule (\n"
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + " immatriculation TEXT,\n"
                + " marque TEXT NOT NULL,\n"
                + " modele TEXT NOT NULL,\n"
                + " kilometrage_actuel INTEGER NOT NULL,\n"
                + " kilometrage_annuel_estime INTEGER NOT NULL,\n"
                + " image_url TEXT,\n"
                + " image_locale TEXT\n"
                + ");";

        String sqlEntretien = "CREATE TABLE IF NOT EXISTS Entretien_Fait (\n"
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + " vehicule_id INTEGER NOT NULL,\n"
                + " type_operation TEXT NOT NULL,\n"
                + " date_realisation DATE NOT NULL,\n"
                + " kilometrage_realisation INTEGER NOT NULL,\n"
                + " cout REAL NOT NULL,\n"
                + " FOREIGN KEY (vehicule_id) REFERENCES Vehicule(id)\n"
                + ");";

        String sqlPreconisation = "CREATE TABLE IF NOT EXISTS Preconisation (\n"
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + " type_operation TEXT NOT NULL,\n"
                + " intervalle_km INTEGER NOT NULL,\n"
                + " intervalle_mois INTEGER NOT NULL\n"
                + ");";

        String sqlEntretienPlanifie = "CREATE TABLE IF NOT EXISTS Entretien_Planifie (\n"
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + " vehicule_id INTEGER NOT NULL,\n"
                + " type_operation TEXT NOT NULL,\n"
                + " date_prevue DATE NOT NULL,\n"
                + " note TEXT,\n"
                + " FOREIGN KEY (vehicule_id) REFERENCES Vehicule(id)\n"
                + ");";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sqlVehicule);
            stmt.execute(sqlEntretien);
            stmt.execute(sqlPreconisation);
            stmt.execute(sqlEntretienPlanifie);

            ajouterColonneSiAbsente(conn, "Vehicule", "immatriculation", "TEXT");
            ajouterColonneSiAbsente(conn, "Vehicule", "image_url", "TEXT");
            ajouterColonneSiAbsente(conn, "Vehicule", "image_locale", "TEXT");

            System.out.println("Base de données initialisée avec succès.");
        } catch (SQLException e) {
            System.out.println("Erreur lors de la création de la table : " + e.getMessage());
        }
    }

    /**
     * Ajoute une colonne à une table seulement si elle n'y est pas déjà (SQLite n'a pas de
     * {@code ADD COLUMN IF NOT EXISTS}). Les noms viennent du code, jamais de l'utilisateur.
     */
    private static void ajouterColonneSiAbsente(Connection conn, String table, String colonne, String type) throws SQLException {
        boolean presente = false;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                if (colonne.equalsIgnoreCase(rs.getString("name"))) {
                    presente = true;
                    break;
                }
            }
        }
        if (!presente) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE " + table + " ADD COLUMN " + colonne + " " + type);
                System.out.println("Colonne ajoutée : " + table + "." + colonne);
            }
        }
    }
}
