package com.carnetentretien.carnetentretien.db;

import com.carnetentretien.carnetentretien.model.Preconisation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class PreconisationDB {

    public static void ajouterPreconisation(Preconisation preconisation) {
        String sql = "INSERT INTO Preconisation(type_operation, intervalle_km, intervalle_mois) VALUES(?,?,?)";

        try (Connection conn = DatabaseManager.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, preconisation.getTypeOperation());
            pstmt.setInt(2, preconisation.getIntervalleKm());
            pstmt.setInt(3, preconisation.getIntervalleMois());

            pstmt.executeUpdate();
            System.out.println("Préconisation ajoutée !");

        } catch (SQLException e) {
            System.out.println("Erreur (ajout préconisation) : " + e.getMessage());
        }
    }

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
            System.out.println("Erreur (récupération préconisations) : " + e.getMessage());
        }
        return liste;
    }

    public static void supprimerPreconisation(int id) {
        String sql = "DELETE FROM Preconisation WHERE id = ?";
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur (suppression préconisation) : " + e.getMessage());
        }
    }

    public static void modifierPreconisation(Preconisation preconisation) {
        String sql = "UPDATE Preconisation SET type_operation = ?, intervalle_km = ?, intervalle_mois = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, preconisation.getTypeOperation());
            pstmt.setInt(2, preconisation.getIntervalleKm());
            pstmt.setInt(3, preconisation.getIntervalleMois());
            pstmt.setInt(4, preconisation.getId());

            pstmt.executeUpdate();
            System.out.println("Préconisation modifiée !");
        } catch (SQLException e) {
            System.out.println("Erreur (modification préconisation) : " + e.getMessage());
        }
    }
}