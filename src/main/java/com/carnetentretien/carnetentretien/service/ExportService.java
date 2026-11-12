package com.carnetentretien.carnetentretien.service;

import com.carnetentretien.carnetentretien.db.ControleTechniqueDB;
import com.carnetentretien.carnetentretien.db.EntretienDB;
import com.carnetentretien.carnetentretien.db.EntretienPlanifieDB;
import com.carnetentretien.carnetentretien.db.PreconisationDB;
import com.carnetentretien.carnetentretien.db.ReleveKilometrageDB;
import com.carnetentretien.carnetentretien.model.ControleTechnique;
import com.carnetentretien.carnetentretien.model.Entretien;
import com.carnetentretien.carnetentretien.model.EntretienPlanifie;
import com.carnetentretien.carnetentretien.model.Preconisation;
import com.carnetentretien.carnetentretien.model.ReleveKilometrage;
import com.carnetentretien.carnetentretien.model.Vehicule;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;

/**
 * Export complet d'un véhicule : affiche la fenêtre « Enregistrer sous » puis délègue l'écriture
 * à {@link CsvHistorique}. À appeler depuis le thread JavaFX.
 */
public class ExportService {

    /**
     * Demande à l'utilisateur où enregistrer le fichier CSV puis écrit un export complet
     * (relevés kilométriques, entretiens réalisés, planifiés, contrôles techniques et échéances).
     * Ne fait rien s'il annule.
     *
     * @param vehicule véhicule concerné
     * @return {@code true} si le fichier a été écrit, {@code false} si l'utilisateur a annulé ou en cas d'erreur
     */
    public static boolean exporterHistoriqueCSV(Vehicule vehicule) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter le carnet");
        String nom = (vehicule.getMarque() + "_" + vehicule.getModele()).replaceAll("\\s+", "_");
        fileChooser.setInitialFileName("Carnet_" + nom + ".csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers Excel/CSV (*.csv)", "*.csv"));

        File fichier = fileChooser.showSaveDialog(new Stage());
        if (fichier == null) return false;

        try {
            List<ReleveKilometrage> releves = ReleveKilometrageDB.getRelevesPourVehicule(vehicule.getId());
            List<Entretien> historique = EntretienDB.getEntretiensPourVehicule(vehicule.getId());
            List<EntretienPlanifie> planifies = EntretienPlanifieDB.getEntretiensPlanifiesPourVehicule(vehicule.getId());
            List<ControleTechnique> controles = ControleTechniqueDB.getControlesPourVehicule(vehicule.getId());
            List<Preconisation> regles = PreconisationDB.getToutesLesPreconisations();
            List<CalculateurEntretien.Bilan> bilans = CalculateurEntretien.analyserEntretien(vehicule, historique, regles);
            ControleTechniqueService.Suivi suiviCt = ControleTechniqueService.analyser(controles);

            CsvHistorique.ecrire(fichier, vehicule, releves, historique, planifies, controles, bilans, suiviCt);
            return true;
        } catch (Exception e) {
            System.err.println("Erreur lors de l'export : " + e.getMessage());
            return false;
        }
    }
}
