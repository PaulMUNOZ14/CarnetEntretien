package com.carnetentretien.carnetentretien.db;

import com.carnetentretien.carnetentretien.model.Entretien;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EntretienDB {

    public static void ajouterEntretien(Entretien entretien) {
        String sql = "INSERT INTO Entretien_Fait(vehicule_id, type_operation, date_realisation, kilometrage_realisation, cout) VALUES(?,?,?,?,?)";

        try (Connection conn = DatabaseManager.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, entretien.getVehiculeId());
            pstmt.setString(2, entretien.getTypeOperation());
            pstmt.setDate(3, Date.valueOf(entretien.getDateRealisation()));
            pstmt.setInt(4, entretien.getKilometrageRealisation());
            pstmt.setDouble(5, entretien.getCout());

            pstmt.executeUpdate();
            System.out.println("Entretien ajouté avec succès !");

        } catch (SQLException e) {
            System.out.println("Erreur lors de l'ajout de l'entretien : " + e.getMessage());
        }
    }

    public static List<Entretien> getEntretiensPourVehicule(int vehiculeId) {
        List<Entretien> liste = new ArrayList<>();
        String sql = "SELECT * FROM Entretien_Fait WHERE vehicule_id = ?";

        try (Connection conn = DatabaseManager.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, vehiculeId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Entretien e = new Entretien(
                        rs.getInt("id"),
                        rs.getInt("vehicule_id"),
                        rs.getString("type_operation"),
                        rs.getDate("date_realisation").toLocalDate(),
                        rs.getInt("kilometrage_realisation"),
                        rs.getDouble("cout")
                );
                liste.add(e);
            }
        } catch (SQLException e) {
            System.out.println("Erreur de récupération : " + e.getMessage());
        }
        return liste;
    }
}