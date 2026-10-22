package com.carnetentretien.carnetentretien.service;

import com.carnetentretien.carnetentretien.model.Entretien;
import com.carnetentretien.carnetentretien.model.Vehicule;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;

public class ExportService {

    public static void exporterHistoriqueCSV(Vehicule vehicule, List<Entretien> historique) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter l'historique");
        fileChooser.setInitialFileName("Historique_" + vehicule.getMarque().replace(" ", "_") + ".csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers Excel/CSV (*.csv)", "*.csv"));

        File fichier = fileChooser.showSaveDialog(new Stage());

        if (fichier != null) {
            try (PrintWriter writer = new PrintWriter(fichier, "UTF-8")) {
                writer.write('\ufeff');
                writer.println("Véhicule :;" + vehicule.getMarque() + " " + vehicule.getModele());
                writer.println("Date;Opération;Kilométrage;Coût (€)");

                double coutTotal = 0;

                for (Entretien e : historique) {
                    writer.println(e.getDateRealisation() + ";" + e.getTypeOperation() + ";" + e.getKilometrageRealisation() + ";" + e.getCout());
                    coutTotal += e.getCout();
                }

                writer.println(";;Total :;" + coutTotal);
                System.out.println("Export réussi : " + fichier.getAbsolutePath());
            } catch (Exception e) {
                System.out.println("Erreur lors de l'export : " + e.getMessage());
            }
        }
    }
}