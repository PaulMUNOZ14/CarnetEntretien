package com.carnetentretien.carnetentretien.db;

import com.carnetentretien.carnetentretien.model.EntretienPlanifie;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Accès à la table {@code Entretien_Planifie} : les entretiens que l'utilisateur a planifiés
 * depuis le calendrier. Même gestion d'erreurs que {@link EntretienDB}.
 */
public class EntretienPlanifieDB {

    /** Enregistre une planification pour le véhicule {@code entretien.getVehiculeId()}. */
    public static void ajouterEntretienPlanifie(EntretienPlanifie entretien) {
        String sql = "INSERT INTO Entretien_Planifie(vehicule_id, type_operation, date_prevue, note) VALUES(?,?,?,?)";

        try (Connection conn = DatabaseManager.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, entretien.getVehiculeId());
            pstmt.setString(2, entretien.getTypeOperation());
            pstmt.setDate(3, Date.valueOf(entretien.getDatePrevue()));
            pstmt.setString(4, entretien.getNote());

            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout de l'entretien planifié : " + e.getMessage());
        }
    }

    /** @return les planifications d'un véhicule, sans ordre garanti (liste vide si aucune) */
    public static List<EntretienPlanifie> getEntretiensPlanifiesPourVehicule(int vehiculeId) {
        List<EntretienPlanifie> liste = new ArrayList<>();
        String sql = "SELECT * FROM Entretien_Planifie WHERE vehicule_id = ?";

        try (Connection conn = DatabaseManager.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, vehiculeId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                liste.add(lire(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur de récupération des entretiens planifiés : " + e.getMessage());
        }
        return liste;
    }

    /** @return toutes les planifications, tous véhicules confondus (liste vide s'il n'y en a aucune) */
    public static List<EntretienPlanifie> getTousLesEntretiensPlanifies() {
        List<EntretienPlanifie> liste = new ArrayList<>();
        String sql = "SELECT * FROM Entretien_Planifie";

        try (Connection conn = DatabaseManager.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                liste.add(lire(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur de récupération des entretiens planifiés : " + e.getMessage());
        }
        return liste;
    }

    /** Met à jour la planification dont l'id est celui de l'objet (le véhicule associé peut changer). */
    public static void modifierEntretienPlanifie(EntretienPlanifie entretien) {
        String sql = "UPDATE Entretien_Planifie SET vehicule_id = ?, type_operation = ?, date_prevue = ?, note = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, entretien.getVehiculeId());
            pstmt.setString(2, entretien.getTypeOperation());
            pstmt.setDate(3, Date.valueOf(entretien.getDatePrevue()));
            pstmt.setString(4, entretien.getNote());
            pstmt.setInt(5, entretien.getId());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur de modification de l'entretien planifié : " + e.getMessage());
        }
    }

    /** Supprime une planification ; sans effet si l'id n'existe pas. */
    public static void supprimerEntretienPlanifie(int id) {
        String sql = "DELETE FROM Entretien_Planifie WHERE id = ?";

        try (Connection conn = DatabaseManager.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur de suppression de l'entretien planifié : " + e.getMessage());
        }
    }

    /** Supprime toutes les planifications d'un véhicule (appelé avec la suppression du véhicule). */
    public static void supprimerEntretiensPlanifiesPourVehicule(int vehiculeId) {
        String sql = "DELETE FROM Entretien_Planifie WHERE vehicule_id = ?";

        try (Connection conn = DatabaseManager.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, vehiculeId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur de suppression des entretiens planifiés : " + e.getMessage());
        }
    }

    private static EntretienPlanifie lire(ResultSet rs) throws SQLException {
        LocalDate date = rs.getDate("date_prevue").toLocalDate();
        return new EntretienPlanifie(
                rs.getInt("id"),
                rs.getInt("vehicule_id"),
                rs.getString("type_operation"),
                date,
                rs.getString("note")
        );
    }
}
