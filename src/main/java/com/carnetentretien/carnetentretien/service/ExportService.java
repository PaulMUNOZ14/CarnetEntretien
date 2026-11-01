package com.carnetentretien.carnetentretien.service;

import com.carnetentretien.carnetentretien.model.Entretien;
import com.carnetentretien.carnetentretien.model.Vehicule;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;

/**
 * Export de l'historique d'un véhicule : affiche la fenêtre « Enregistrer sous » puis délègue l'écriture
 * à {@link CsvHistorique}. À appeler depuis le thread JavaFX.
 */
public class ExportService {

    /**
     * Demande à l'utilisateur où enregistrer le fichier CSV puis l'écrit. Ne fait rien s'il annule ;
     * en cas d'erreur d'écriture, elle est affichée dans la console.
     *
     * @param vehicule   véhicule concerné (nom du fichier proposé et en-tête)
     * @param historique entretiens à exporter
     */
    public static void exporterHistoriqueCSV(Vehicule vehicule, List<Entretien> historique) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter l'historique");
        fileChooser.setInitialFileName("Historique_" + vehicule.getMarque().replace(" ", "_") + ".csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers Excel/CSV (*.csv)", "*.csv"));

        File fichier = fileChooser.showSaveDialog(new Stage());

        if (fichier != null) {
            try {
                CsvHistorique.ecrire(fichier, vehicule, historique);
                System.out.println("Export réussi : " + fichier.getAbsolutePath());
            } catch (Exception e) {
                System.out.println("Erreur lors de l'export : " + e.getMessage());
            }
        }
    }
}
