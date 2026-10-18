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
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.layout.GridPane;

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

    @FXML
    public void supprimerVehiculeSelectionne() {
        if (vehiculeSelectionne != null) {
            VehiculeDB.supprimerVehicule(vehiculeSelectionne.getId());

            panneauDetails.setDisable(true);
            titreDetails.setText("Sélectionnez un véhicule");
            listeEntretiensUI.setItems(FXCollections.observableArrayList());
            vehiculeSelectionne = null;

            chargerVehicules();
        }
    }

    @FXML
    public void modifierVehiculeSelectionne() {
        if (vehiculeSelectionne != null) {
            Dialog<Vehicule> dialog = new Dialog<>();
            dialog.setTitle("Modifier le véhicule");
            dialog.setHeaderText("Modifiez les informations de : " + vehiculeSelectionne.getMarque() + " " + vehiculeSelectionne.getModele());

            ButtonType btnSauvegarder = new ButtonType("Sauvegarder", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(btnSauvegarder, ButtonType.CANCEL);

            GridPane grid = new GridPane();
            grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20, 20, 10, 10));

            TextField editMarque = new TextField(vehiculeSelectionne.getMarque());
            TextField editModele = new TextField(vehiculeSelectionne.getModele());
            TextField editKmActuel = new TextField(String.valueOf(vehiculeSelectionne.getKilometrageActuel()));
            TextField editKmAnnuel = new TextField(String.valueOf(vehiculeSelectionne.getKilometrageAnnuelEstime()));

            grid.add(new Label("Marque :"), 0, 0); grid.add(editMarque, 1, 0);
            grid.add(new Label("Modèle :"), 0, 1); grid.add(editModele, 1, 1);
            grid.add(new Label("Km Actuel :"), 0, 2); grid.add(editKmActuel, 1, 2);
            grid.add(new Label("Km Annuel :"), 0, 3); grid.add(editKmAnnuel, 1, 3);

            dialog.getDialogPane().setContent(grid);

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == btnSauvegarder) {
                    try {
                        return new Vehicule(vehiculeSelectionne.getId(), editMarque.getText(), editModele.getText(),
                                Integer.parseInt(editKmActuel.getText()), Integer.parseInt(editKmAnnuel.getText()));
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
                return null;
            });

            dialog.showAndWait().ifPresent(vehiculeModifie -> {
                VehiculeDB.modifierVehicule(vehiculeModifie);
                chargerVehicules();

                afficherDetails(vehiculeModifie);
            });
        }
    }
}