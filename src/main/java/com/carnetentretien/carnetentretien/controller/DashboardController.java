package com.carnetentretien.carnetentretien.controller;

import com.carnetentretien.carnetentretien.db.VehiculeDB;
import com.carnetentretien.carnetentretien.model.Vehicule;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;

public class DashboardController {

    @FXML
    private ListView<String> listeVehiculesUI;

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
}