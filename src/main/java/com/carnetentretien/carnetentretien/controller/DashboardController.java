package com.carnetentretien.carnetentretien.controller;

import com.carnetentretien.carnetentretien.db.EntretienDB;
import com.carnetentretien.carnetentretien.db.VehiculeDB;
import com.carnetentretien.carnetentretien.model.Entretien;
import com.carnetentretien.carnetentretien.model.Vehicule;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class DashboardController {

    @FXML private ListView<Vehicule> listeVehiculesUI;
    @FXML private TextField champMarque;
    @FXML private TextField champModele;
    @FXML private TextField champKmActuel;
    @FXML private TextField champKmAnnuel;
    @FXML private VBox panneauDetails;
    @FXML private Label titreDetails;
    @FXML private ListView<Entretien> listeEntretiensUI;
    @FXML private TextField champOperation;
    @FXML private DatePicker champDate;
    @FXML private TextField champKmRealisation;
    @FXML private TextField champCout;

    private Vehicule vehiculeSelectionne = null;

    @FXML
    public void initialize() {
        chargerVehicules();

        listeVehiculesUI.getSelectionModel().selectedItemProperty().addListener((observable, oldSelection, newSelection) -> {
            if (newSelection != null) {
                vehiculeSelectionne = newSelection;
                afficherDetails(vehiculeSelectionne);
            }
        });
    }

    private void chargerVehicules() {
        ObservableList<Vehicule> items = FXCollections.observableArrayList(VehiculeDB.getTousLesVehicules());
        listeVehiculesUI.setItems(items);
    }

    private void afficherDetails(Vehicule v) {
        panneauDetails.setDisable(false);
        titreDetails.setText("Historique : " + v.getMarque() + " " + v.getModele());

        ObservableList<Entretien> factures = FXCollections.observableArrayList(EntretienDB.getEntretiensPourVehicule(v.getId()));
        listeEntretiensUI.setItems(factures);
    }

    @FXML
    public void ajouterNouveauVehicule() {
        try {
            Vehicule v = new Vehicule(0, champMarque.getText(), champModele.getText(),
                    Integer.parseInt(champKmActuel.getText()), Integer.parseInt(champKmAnnuel.getText()));
            VehiculeDB.ajouterVehicule(v);
            chargerVehicules();
            champMarque.clear(); champModele.clear(); champKmActuel.clear(); champKmAnnuel.clear();
        } catch (NumberFormatException e) {
            System.out.println("Erreur : Kilomètres invalides.");
        }
    }

    @FXML
    public void ajouterNouvelEntretien() {
        if (vehiculeSelectionne == null || champDate.getValue() == null) return;

        try {
            Entretien e = new Entretien(0, vehiculeSelectionne.getId(), champOperation.getText(),
                    champDate.getValue(), Integer.parseInt(champKmRealisation.getText()),
                    Double.parseDouble(champCout.getText()));

            EntretienDB.ajouterEntretien(e);

            afficherDetails(vehiculeSelectionne);

            champOperation.clear(); champKmRealisation.clear(); champCout.clear(); champDate.setValue(null);

        } catch (NumberFormatException ex) {
            System.out.println("Erreur : Valeurs numériques invalides pour l'entretien.");
        }
    }
}