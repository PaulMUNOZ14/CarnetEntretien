package com.carnetentretien.carnetentretien.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    // Le chemin vers le fichier de la base de données
    private static final String URL = "jdbc:sqlite:carnet_entretien.db";

    public static Connection connect() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(URL);
        } catch (SQLException e) {
            System.out.println("Erreur de connexion à SQLite : " + e.getMessage());
        }
        return conn;
    }

    public static void initDatabase() {
        // Requête SQL pour créer la table Vehicule si elle n'existe pas
        String sql = "CREATE TABLE IF NOT EXISTS Vehicule (\n"
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + " marque TEXT NOT NULL,\n"
                + " modele TEXT NOT NULL,\n"
                + " kilometrage_actuel INTEGER NOT NULL,\n"
                + " kilometrage_annuel_estime INTEGER NOT NULL\n"
                + ");";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Base de données initialisée avec succès.");
        } catch (SQLException e) {
            System.out.println("Erreur lors de la création de la table : " + e.getMessage());
        }
    }
}