package com.carnetentretien.carnetentretien.db;

import com.carnetentretien.carnetentretien.model.ControleTechnique;

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
 * Accès à la table {@code ControleTechnique} : l'historique des contrôles techniques.
 * Même gestion d'erreurs que {@link EntretienDB}.
 */
public class ControleTechniqueDB {

    /** Enregistre un contrôle technique pour le véhicule {@code controle.getVehiculeId()}. */
    public static void ajouterControleTechnique(ControleTechnique controle) {
        String sql = "INSERT INTO ControleTechnique(vehicule_id, date_realisation, resultat, date_prochaine, note) VALUES(?,?,?,?,?)";

        try (Connection conn = DatabaseManager.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, controle.getVehiculeId());
            pstmt.setDate(2, Date.valueOf(controle.getDateRealisation()));
            pstmt.setString(3, controle.getResultat().name());
            pstmt.setDate(4, Date.valueOf(controle.getDateProchaine()));
            pstmt.setString(5, controle.getNote());

            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout du contrôle technique : " + e.getMessage());
        }
    }

    /** @return les contrôles d'un véhicule, du plus récent au plus ancien (liste vide s'il n'y en a aucun) */
    public static List<ControleTechnique> getControlesPourVehicule(int vehiculeId) {
        List<ControleTechnique> liste = new ArrayList<>();
        String sql = "SELECT * FROM ControleTechnique WHERE vehicule_id = ? ORDER BY date_realisation DESC, id DESC";

        try (Connection conn = DatabaseManager.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, vehiculeId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                liste.add(lire(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur de récupération des contrôles techniques : " + e.getMessage());
        }
        return liste;
    }

    /** @return tous les contrôles, tous véhicules confondus (liste vide s'il n'y en a aucun) */
    public static List<ControleTechnique> getTousLesControles() {
        List<ControleTechnique> liste = new ArrayList<>();
        String sql = "SELECT * FROM ControleTechnique ORDER BY date_realisation DESC, id DESC";

        try (Connection conn = DatabaseManager.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                liste.add(lire(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur de récupération des contrôles techniques : " + e.getMessage());
        }
        return liste;
    }

    /** Met à jour le contrôle dont l'id est celui de l'objet (le véhicule associé peut changer). */
    public static void modifierControleTechnique(ControleTechnique controle) {
        String sql = "UPDATE ControleTechnique SET vehicule_id = ?, date_realisation = ?, resultat = ?, "
                + "date_prochaine = ?, note = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, controle.getVehiculeId());
            pstmt.setDate(2, Date.valueOf(controle.getDateRealisation()));
            pstmt.setString(3, controle.getResultat().name());
            pstmt.setDate(4, Date.valueOf(controle.getDateProchaine()));
            pstmt.setString(5, controle.getNote());
            pstmt.setInt(6, controle.getId());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur de modification du contrôle technique : " + e.getMessage());
        }
    }

    /** Supprime un contrôle ; sans effet si l'id n'existe pas. */
    public static void supprimerControleTechnique(int id) {
        String sql = "DELETE FROM ControleTechnique WHERE id = ?";

        try (Connection conn = DatabaseManager.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur de suppression du contrôle technique : " + e.getMessage());
        }
    }

    /** Supprime tous les contrôles d'un véhicule (appelé avec la suppression du véhicule). */
    public static void supprimerControlesPourVehicule(int vehiculeId) {
        String sql = "DELETE FROM ControleTechnique WHERE vehicule_id = ?";

        try (Connection conn = DatabaseManager.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, vehiculeId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur de suppression des contrôles techniques : " + e.getMessage());
        }
    }

    private static ControleTechnique lire(ResultSet rs) throws SQLException {
        LocalDate realisation = rs.getDate("date_realisation").toLocalDate();
        LocalDate prochaine = rs.getDate("date_prochaine").toLocalDate();
        return new ControleTechnique(
                rs.getInt("id"),
                rs.getInt("vehicule_id"),
                realisation,
                lireResultat(rs.getString("resultat")),
                prochaine,
                rs.getString("note")
        );
    }

    private static ControleTechnique.Resultat lireResultat(String texte) {
        try {
            return ControleTechnique.Resultat.valueOf(texte);
        } catch (IllegalArgumentException | NullPointerException e) {
            // Valeur inconnue (base ancienne ou modifiée à la main) : on ne bloque pas l'affichage.
            return ControleTechnique.Resultat.FAVORABLE;
        }
    }
}
