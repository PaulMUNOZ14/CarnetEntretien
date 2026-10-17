package com.carnetentretien.carnetentretien.controller;

import com.carnetentretien.carnetentretien.db.VehiculeDB;
import com.carnetentretien.carnetentretien.model.Vehicule;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class DashboardController {

    @FXML private ListView<String> listeVehiculesUI;

    @FXML private TextField champMarque;
    @FXML private TextField champModele;
    @FXML private TextField champKmActuel;
    @FXML private TextField champKmAnnuel;

    @FXML
    public void initialize() {
        chargerVehicules();
    }

    private void chargerVehicules() {
        ObservableList<String> items = FXCollections.observableArrayList();
        for (Vehicule v : VehiculeDB.getTousLesVehicules()) {
            items.add(v.getMarque() + " " + v.getModele() + " - " + v.getKilometrageActuel() + " km");
        }
        listeVehiculesUI.setItems(items);
    }

    @FXML
    public void ajouterNouveauVehicule() {
        try {
            String marque = champMarque.getText();
            String modele = champModele.getText();
            int kmActuel = Integer.parseInt(champKmActuel.getText());
            int kmAnnuel = Integer.parseInt(champKmAnnuel.getText());

            Vehicule nouveauVehicule = new Vehicule(0, marque, modele, kmActuel, kmAnnuel);
            VehiculeDB.ajouterVehicule(nouveauVehicule);

            chargerVehicules();
            champMarque.clear();
            champModele.clear();
            champKmActuel.clear();
            champKmAnnuel.clear();

        } catch (NumberFormatException e) {
            System.out.println("Erreur : Veuillez entrer des nombres valides pour les kilomètres.");
        }
    }
}