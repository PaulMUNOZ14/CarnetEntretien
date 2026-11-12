package com.carnetentretien.carnetentretien.db;

import com.carnetentretien.carnetentretien.model.ReleveKilometrage;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/**
 * Accès à la table {@code Releve_Kilometrage} : l'historique des relevés du compteur.
 * <p>
 * Après chaque ajout, modification ou suppression, le kilométrage actuel du véhicule est
 * recalculé à partir du relevé le plus récent. Même gestion d'erreurs que {@link VehiculeDB}.
 */
public class ReleveKilometrageDB {

    /** Enregistre un relevé pour le véhicule {@code releve.getVehiculeId()}. */
    public static void ajouterReleve(ReleveKilometrage releve) {
        String sql = "INSERT INTO Releve_Kilometrage(vehicule_id, date_releve, kilometrage, note) VALUES(?,?,?,?)";
        boolean ajoute = false;

        try (Connection conn = DatabaseManager.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, releve.getVehiculeId());
            pstmt.setDate(2, Date.valueOf(releve.getDateReleve()));
            pstmt.setInt(3, releve.getKilometrage());
            pstmt.setString(4, releve.getNote());

            ajoute = pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout du relevé kilométrique : " + e.getMessage());
        }

        if (ajoute) synchroniserKilometrage(releve.getVehiculeId());
    }

    /** @return les relevés d'un véhicule, du plus récent au plus ancien (même date : le dernier saisi d'abord) */
    public static List<ReleveKilometrage> getRelevesPourVehicule(int vehiculeId) {
        List<ReleveKilometrage> liste = new ArrayList<>();
        String sql = "SELECT * FROM Releve_Kilometrage WHERE vehicule_id = ? ORDER BY date_releve DESC, id DESC";

        try (Connection conn = DatabaseManager.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, vehiculeId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    liste.add(lire(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur de récupération des relevés kilométriques : " + e.getMessage());
        }
        return liste;
    }

    /** @return tous les relevés, tous véhicules confondus (liste vide s'il n'y en a aucun) */
    public static List<ReleveKilometrage> getTousLesReleves() {
        List<ReleveKilometrage> liste = new ArrayList<>();
        String sql = "SELECT * FROM Releve_Kilometrage ORDER BY date_releve DESC, id DESC";

        try (Connection conn = DatabaseManager.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                liste.add(lire(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur de récupération des relevés kilométriques : " + e.getMessage());
        }
        return liste;
    }

    /** Met à jour le relevé dont l'id est celui de l'objet (le véhicule associé ne change pas). */
    public static void modifierReleve(ReleveKilometrage releve) {
        String sql = "UPDATE Releve_Kilometrage SET date_releve = ?, kilometrage = ?, note = ? WHERE id = ?";
        boolean modifie = false;

        try (Connection conn = DatabaseManager.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDate(1, Date.valueOf(releve.getDateReleve()));
            pstmt.setInt(2, releve.getKilometrage());
            pstmt.setString(3, releve.getNote());
            pstmt.setInt(4, releve.getId());

            modifie = pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur de modification du relevé kilométrique : " + e.getMessage());
        }

        if (modifie) synchroniserKilometrage(releve.getVehiculeId());
    }

    /** Supprime un relevé ; sans effet si l'id n'existe pas. */
    public static void supprimerReleve(int id) {
        Integer vehiculeId = vehiculeIdDuReleve(id);
        if (vehiculeId == null) return;

        String sql = "DELETE FROM Releve_Kilometrage WHERE id = ?";
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur de suppression du relevé kilométrique : " + e.getMessage());
        }

        synchroniserKilometrage(vehiculeId);
    }

    /** Supprime tous les relevés d'un véhicule (appelé avec la suppression du véhicule). */
    public static void supprimerRelevesPourVehicule(int vehiculeId) {
        String sql = "DELETE FROM Releve_Kilometrage WHERE vehicule_id = ?";

        try (Connection conn = DatabaseManager.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, vehiculeId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur de suppression des relevés kilométriques : " + e.getMessage());
        }
    }

    /**
     * @return le kilométrage du relevé le plus récent du véhicule,
     *         ou vide s'il n'y a encore aucun relevé
     */
    public static OptionalInt getDernierKilometrage(int vehiculeId) {
        String sql = "SELECT kilometrage FROM Releve_Kilometrage WHERE vehicule_id = ? "
                + "ORDER BY date_releve DESC, id DESC LIMIT 1";

        try (Connection conn = DatabaseManager.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, vehiculeId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return OptionalInt.of(rs.getInt(1));
            }
        } catch (SQLException e) {
            System.err.println("Erreur de lecture du dernier relevé kilométrique : " + e.getMessage());
        }
        return OptionalInt.empty();
    }

    /**
     * Aligne le kilométrage actuel du véhicule sur son relevé le plus récent.
     * Sans effet s'il n'y a aucun relevé (on ne veut pas effacer une valeur existante).
     */
    public static void synchroniserKilometrage(int vehiculeId) {
        OptionalInt dernier = getDernierKilometrage(vehiculeId);
        if (dernier.isEmpty()) return;

        String sql = "UPDATE Vehicule SET kilometrage_actuel = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, dernier.getAsInt());
            pstmt.setInt(2, vehiculeId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur de mise à jour du kilométrage actuel : " + e.getMessage());
        }
    }

    private static Integer vehiculeIdDuReleve(int id) {
        String sql = "SELECT vehicule_id FROM Releve_Kilometrage WHERE id = ?";

        try (Connection conn = DatabaseManager.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Erreur de lecture du relevé kilométrique : " + e.getMessage());
        }
        return null;
    }

    private static ReleveKilometrage lire(ResultSet rs) throws SQLException {
        return new ReleveKilometrage(
                rs.getInt("id"),
                rs.getInt("vehicule_id"),
                rs.getDate("date_releve").toLocalDate(),
                rs.getInt("kilometrage"),
                rs.getString("note")
        );
    }
}
