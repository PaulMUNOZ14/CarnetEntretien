package com.carnetentretien.carnetentretien.db;

import com.carnetentretien.carnetentretien.model.ControleTechniquePlanifie;

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
 * Accès à la table {@code ControleTechnique_Planifie} : les contrôles techniques que l'utilisateur
 * a planifiés depuis le calendrier. Même gestion d'erreurs que {@link ControleTechniqueDB}.
 */
public class ControleTechniquePlanifieDB {

    /** Enregistre une planification pour le véhicule {@code controle.getVehiculeId()}. */
    public static void ajouterControleTechniquePlanifie(ControleTechniquePlanifie controle) {
        String sql = "INSERT INTO ControleTechnique_Planifie(vehicule_id, date_prevue, note) VALUES(?,?,?)";

        try (Connection conn = DatabaseManager.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, controle.getVehiculeId());
            pstmt.setDate(2, Date.valueOf(controle.getDatePrevue()));
            pstmt.setString(3, controle.getNote());

            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout du contrôle technique planifié : " + e.getMessage());
        }
    }

    /** @return les contrôles planifiés d'un véhicule, sans ordre garanti (liste vide si aucun) */
    public static List<ControleTechniquePlanifie> getControlesPlanifiesPourVehicule(int vehiculeId) {
        List<ControleTechniquePlanifie> liste = new ArrayList<>();
        String sql = "SELECT * FROM ControleTechnique_Planifie WHERE vehicule_id = ?";

        try (Connection conn = DatabaseManager.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, vehiculeId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                liste.add(lire(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur de récupération des contrôles techniques planifiés : " + e.getMessage());
        }
        return liste;
    }

    /** @return toutes les planifications de contrôles, tous véhicules confondus (liste vide s'il n'y en a aucune) */
    public static List<ControleTechniquePlanifie> getTousLesControlesPlanifies() {
        List<ControleTechniquePlanifie> liste = new ArrayList<>();
        String sql = "SELECT * FROM ControleTechnique_Planifie";

        try (Connection conn = DatabaseManager.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                liste.add(lire(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur de récupération des contrôles techniques planifiés : " + e.getMessage());
        }
        return liste;
    }

    /** Met à jour la planification dont l'id est celui de l'objet (le véhicule associé peut changer). */
    public static void modifierControleTechniquePlanifie(ControleTechniquePlanifie controle) {
        String sql = "UPDATE ControleTechnique_Planifie SET vehicule_id = ?, date_prevue = ?, note = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, controle.getVehiculeId());
            pstmt.setDate(2, Date.valueOf(controle.getDatePrevue()));
            pstmt.setString(3, controle.getNote());
            pstmt.setInt(4, controle.getId());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur de modification du contrôle technique planifié : " + e.getMessage());
        }
    }

    /** Supprime une planification ; sans effet si l'id n'existe pas. */
    public static void supprimerControleTechniquePlanifie(int id) {
        String sql = "DELETE FROM ControleTechnique_Planifie WHERE id = ?";

        try (Connection conn = DatabaseManager.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur de suppression du contrôle technique planifié : " + e.getMessage());
        }
    }

    /** Supprime toutes les planifications d'un véhicule (appelé avec la suppression du véhicule). */
    public static void supprimerControlesPlanifiesPourVehicule(int vehiculeId) {
        String sql = "DELETE FROM ControleTechnique_Planifie WHERE vehicule_id = ?";

        try (Connection conn = DatabaseManager.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, vehiculeId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur de suppression des contrôles techniques planifiés : " + e.getMessage());
        }
    }

    private static ControleTechniquePlanifie lire(ResultSet rs) throws SQLException {
        LocalDate date = rs.getDate("date_prevue").toLocalDate();
        return new ControleTechniquePlanifie(
                rs.getInt("id"),
                rs.getInt("vehicule_id"),
                date,
                rs.getString("note")
        );
    }
}
