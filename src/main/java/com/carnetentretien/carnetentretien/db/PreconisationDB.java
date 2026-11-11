package com.carnetentretien.carnetentretien.db;

import com.carnetentretien.carnetentretien.model.Preconisation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Accès à la table {@code Preconisation} : les règles de suivi, communes à tous les véhicules. */
public class PreconisationDB {

    /** Enregistre une nouvelle règle. */
    public static void ajouterPreconisation(Preconisation preconisation) {
        String sql = "INSERT INTO Preconisation(type_operation, intervalle_km, intervalle_mois) VALUES(?,?,?)";

        try (Connection conn = DatabaseManager.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, preconisation.getTypeOperation());
            pstmt.setInt(2, preconisation.getIntervalleKm());
            pstmt.setInt(3, preconisation.getIntervalleMois());

            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Erreur (ajout préconisation) : " + e.getMessage());
        }
    }

    /** @return toutes les règles (liste vide si aucune) */
    public static List<Preconisation> getToutesLesPreconisations() {
        List<Preconisation> liste = new ArrayList<>();
        String sql = "SELECT * FROM Preconisation";

        try (Connection conn = DatabaseManager.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Preconisation p = new Preconisation(
                        rs.getInt("id"),
                        rs.getString("type_operation"),
                        rs.getInt("intervalle_km"),
                        rs.getInt("intervalle_mois")
                );
                liste.add(p);
            }
        } catch (SQLException e) {
            System.err.println("Erreur (récupération préconisations) : " + e.getMessage());
        }
        return liste;
    }

    /** Supprime une règle ; sans effet si l'id n'existe pas. */
    public static void supprimerPreconisation(int id) {
        String sql = "DELETE FROM Preconisation WHERE id = ?";
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur (suppression préconisation) : " + e.getMessage());
        }
    }

    /** Met à jour la règle dont l'id est celui de l'objet. */
    public static void modifierPreconisation(Preconisation preconisation) {
        String sql = "UPDATE Preconisation SET type_operation = ?, intervalle_km = ?, intervalle_mois = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, preconisation.getTypeOperation());
            pstmt.setInt(2, preconisation.getIntervalleKm());
            pstmt.setInt(3, preconisation.getIntervalleMois());
            pstmt.setInt(4, preconisation.getId());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur (modification préconisation) : " + e.getMessage());
        }
    }
}