package com.carnetentretien.carnetentretien.db;

import com.carnetentretien.carnetentretien.model.Vehicule;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Accès à la table {@code Vehicule}. Toutes les méthodes sont statiques ; en cas d'erreur SQL,
 * le message est affiché dans la console et la méthode ne lève pas d'exception.
 */
public class VehiculeDB {

    /** Enregistre un nouveau véhicule (son id est généré par la base ; celui de l'objet est ignoré). */
    public static void ajouterVehicule(Vehicule vehicule) {
        String sql = "INSERT INTO Vehicule(immatriculation, marque, modele, kilometrage_actuel, kilometrage_annuel_estime, image_url, image_locale) " + "VALUES(?,?,?,?,?,?,?)";

        try (Connection conn = DatabaseManager.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, vehicule.getImmatriculation());
            pstmt.setString(2, vehicule.getMarque());
            pstmt.setString(3, vehicule.getModele());
            pstmt.setInt(4, vehicule.getKilometrageActuel());
            pstmt.setInt(5, vehicule.getKilometrageAnnuelEstime());
            pstmt.setString(6, vehicule.getImageUrl());
            pstmt.setString(7, vehicule.getImageLocale());

            pstmt.executeUpdate();
            System.out.println("Véhicule ajouté avec succès en base de données !");

        } catch (SQLException e) {
            System.out.println("Erreur lors de l'ajout du véhicule : " + e.getMessage());
        }
    }

    /** Met à jour tous les champs du véhicule dont l'id est celui de l'objet. */
    public static void modifierVehicule(Vehicule vehicule) {
        String sql = "UPDATE Vehicule SET immatriculation = ?, marque = ?, modele = ?, "
                + "kilometrage_actuel = ?, kilometrage_annuel_estime = ?, image_url = ?, image_locale = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, vehicule.getImmatriculation());
            pstmt.setString(2, vehicule.getMarque());
            pstmt.setString(3, vehicule.getModele());
            pstmt.setInt(4, vehicule.getKilometrageActuel());
            pstmt.setInt(5, vehicule.getKilometrageAnnuelEstime());
            pstmt.setString(6, vehicule.getImageUrl());
            pstmt.setString(7, vehicule.getImageLocale());
            pstmt.setInt(8, vehicule.getId());

            pstmt.executeUpdate();
            System.out.println("Véhicule mis à jour !");
        } catch (SQLException e) {
            System.out.println("Erreur de modification : " + e.getMessage());
        }
    }

    /**
     * Supprime un véhicule <b>et tout son historique d'entretien</b>.
     * Le fichier de la photo personnelle n'est pas touché : voir {@code ImageService#supprimer}.
     */
    public static void supprimerVehicule(int id) {
        String sqlEntretiens = "DELETE FROM Entretien_Fait WHERE vehicule_id = ?";
        String sqlVehicule = "DELETE FROM Vehicule WHERE id = ?";

        try (Connection conn = DatabaseManager.connect();
             PreparedStatement pstmtEntretiens = conn.prepareStatement(sqlEntretiens);
             PreparedStatement pstmtVehicule = conn.prepareStatement(sqlVehicule)) {

            pstmtEntretiens.setInt(1, id);
            pstmtEntretiens.executeUpdate();

            pstmtVehicule.setInt(1, id);
            pstmtVehicule.executeUpdate();

            System.out.println("Véhicule et historique supprimés !");
        } catch (SQLException e) {
            System.out.println("Erreur de suppression : " + e.getMessage());
        }
    }

    /** @return tous les véhicules (liste vide s'il n'y en a pas ou en cas d'erreur) */
    public static List<Vehicule> getTousLesVehicules() {
        List<Vehicule> listeVehicules = new ArrayList<>();
        String sql = "SELECT * FROM Vehicule";

        try (Connection conn = DatabaseManager.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Vehicule v = new Vehicule(
                        rs.getInt("id"),
                        rs.getString("immatriculation"),
                        rs.getString("marque"),
                        rs.getString("modele"),
                        rs.getInt("kilometrage_actuel"),
                        rs.getInt("kilometrage_annuel_estime"),
                        rs.getString("image_url"),
                        rs.getString("image_locale")
                );
                listeVehicules.add(v);
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la récupération des véhicules : " + e.getMessage());
        }
        return listeVehicules;
    }
}
