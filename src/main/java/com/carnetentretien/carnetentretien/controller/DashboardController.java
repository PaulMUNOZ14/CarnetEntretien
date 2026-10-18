package com.carnetentretien.carnetentretien.controller;

import com.carnetentretien.carnetentretien.db.EntretienDB;
import com.carnetentretien.carnetentretien.db.PreconisationDB;
import com.carnetentretien.carnetentretien.db.VehiculeDB;
import com.carnetentretien.carnetentretien.model.Entretien;
import com.carnetentretien.carnetentretien.model.Preconisation;
import com.carnetentretien.carnetentretien.model.Vehicule;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Optional;

public class DashboardController {

    @FXML private ListView<Vehicule> listeVehiculesUI;
    @FXML private TextField champMarque;
    @FXML private TextField champModele;
    @FXML private TextField champKmActuel;
    @FXML private TextField champKmAnnuel;

    @FXML private BorderPane panneauDetails;
    @FXML private Label titreDetails;
    @FXML private ListView<Entretien> listeEntretiensUI;
    @FXML private TextField champOperation;
    @FXML private DatePicker champDate;
    @FXML private TextField champKmRealisation;
    @FXML private TextField champCout;

    private Vehicule vehiculeSelectionne = null;
    private Entretien entretienSelectionne = null;

    @FXML
    public void initialize() {
        chargerVehicules();

        listeVehiculesUI.getSelectionModel().selectedItemProperty().addListener((observable, oldSelection, newSelection) -> {
            if (newSelection != null) {
                vehiculeSelectionne = newSelection;
                afficherDetails(vehiculeSelectionne);
            }
        });

        listeEntretiensUI.getSelectionModel().selectedItemProperty().addListener((observable, oldSelection, newSelection) -> {
            if (newSelection != null) {
                entretienSelectionne = newSelection;
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
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmer la suppression");
            alert.setHeaderText("Supprimer le véhicule " + vehiculeSelectionne.getMarque() + " ?");
            alert.setContentText("Toutes les factures et l'historique associés seront définitivement effacés.");

            Optional<ButtonType> result = alert.showAndWait();

            if (result.isPresent() && result.get() == ButtonType.OK) {
                VehiculeDB.supprimerVehicule(vehiculeSelectionne.getId());

                panneauDetails.setDisable(true);
                titreDetails.setText("Sélectionnez un véhicule");
                listeEntretiensUI.setItems(FXCollections.observableArrayList());
                vehiculeSelectionne = null;

                chargerVehicules();
            }
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

    @FXML
    public void supprimerEntretienSelectionne() {
        if (entretienSelectionne != null && vehiculeSelectionne != null) {

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmer la suppression");
            alert.setHeaderText("Supprimer cette opération ?");
            alert.setContentText("Facture : " + entretienSelectionne.getTypeOperation() + " du " + entretienSelectionne.getDateRealisation());

            Optional<ButtonType> result = alert.showAndWait();

            if (result.isPresent() && result.get() == ButtonType.OK) {
                EntretienDB.supprimerEntretien(entretienSelectionne.getId());
                afficherDetails(vehiculeSelectionne);
                entretienSelectionne = null;
            }
        }
    }

    @FXML
    public void modifierEntretienSelectionne() {
        if (entretienSelectionne != null && vehiculeSelectionne != null) {
            Dialog<Entretien> dialog = new Dialog<>();
            dialog.setTitle("Modifier l'entretien");
            dialog.setHeaderText("Correction de : " + entretienSelectionne.getTypeOperation());

            ButtonType btnSauvegarder = new ButtonType("Sauvegarder", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(btnSauvegarder, ButtonType.CANCEL);

            GridPane grid = new GridPane();
            grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20, 20, 10, 10));

            TextField editOperation = new TextField(entretienSelectionne.getTypeOperation());
            DatePicker editDate = new DatePicker(entretienSelectionne.getDateRealisation());
            TextField editKm = new TextField(String.valueOf(entretienSelectionne.getKilometrageRealisation()));
            TextField editCout = new TextField(String.valueOf(entretienSelectionne.getCout()));

            grid.add(new Label("Opération :"), 0, 0); grid.add(editOperation, 1, 0);
            grid.add(new Label("Date :"), 0, 1); grid.add(editDate, 1, 1);
            grid.add(new Label("Km :"), 0, 2); grid.add(editKm, 1, 2);
            grid.add(new Label("Coût (€) :"), 0, 3); grid.add(editCout, 1, 3);

            dialog.getDialogPane().setContent(grid);

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == btnSauvegarder) {
                    try {
                        return new Entretien(entretienSelectionne.getId(), vehiculeSelectionne.getId(),
                                editOperation.getText(), editDate.getValue(),
                                Integer.parseInt(editKm.getText()), Double.parseDouble(editCout.getText()));
                    } catch (Exception e) {
                        return null;
                    }
                }
                return null;
            });

            dialog.showAndWait().ifPresent(entretienModifie -> {
                EntretienDB.modifierEntretien(entretienModifie);
                afficherDetails(vehiculeSelectionne);
            });
        }
    }

    @FXML
    public void ouvrirParametresPreconisations() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Paramètres d'entretien");
        dialog.setHeaderText("Règles et préconisations constructeur");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(20));

        ListView<Preconisation> listePrecoUI = new ListView<>();
        listePrecoUI.getItems().addAll(PreconisationDB.getToutesLesPreconisations());
        listePrecoUI.setPrefHeight(150);

        Button btnSupprimer = new Button("✖ Supprimer");
        btnSupprimer.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand;");
        btnSupprimer.setOnAction(e -> {
            Preconisation selection = listePrecoUI.getSelectionModel().getSelectedItem();
            if (selection != null) {
                PreconisationDB.supprimerPreconisation(selection.getId());
                listePrecoUI.getItems().setAll(PreconisationDB.getToutesLesPreconisations());
            }
        });

        Button btnModifier = new Button("✎ Modifier");
        btnModifier.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-cursor: hand;");
        btnModifier.setOnAction(e -> {
            Preconisation selection = listePrecoUI.getSelectionModel().getSelectedItem();
            if (selection != null) {
                Dialog<Preconisation> dialogModif = new Dialog<>();
                dialogModif.setTitle("Modifier la règle");
                ButtonType btnSauvegarder = new ButtonType("Sauvegarder", ButtonBar.ButtonData.OK_DONE);
                dialogModif.getDialogPane().getButtonTypes().addAll(btnSauvegarder, ButtonType.CANCEL);

                GridPane grid = new GridPane();
                grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));

                TextField editOp = new TextField(selection.getTypeOperation());
                TextField editKm = new TextField(String.valueOf(selection.getIntervalleKm()));
                TextField editMois = new TextField(String.valueOf(selection.getIntervalleMois()));

                grid.add(new Label("Opération :"), 0, 0); grid.add(editOp, 1, 0);
                grid.add(new Label("Intervalle Km :"), 0, 1); grid.add(editKm, 1, 1);
                grid.add(new Label("Intervalle Mois :"), 0, 2); grid.add(editMois, 1, 2);
                dialogModif.getDialogPane().setContent(grid);

                dialogModif.setResultConverter(dialogButton -> {
                    if (dialogButton == btnSauvegarder) {
                        try {
                            return new Preconisation(selection.getId(), editOp.getText(),
                                    Integer.parseInt(editKm.getText()), Integer.parseInt(editMois.getText()));
                        } catch (NumberFormatException ex) { return null; }
                    }
                    return null;
                });

                dialogModif.showAndWait().ifPresent(pModifiee -> {
                    PreconisationDB.modifierPreconisation(pModifiee);
                    listePrecoUI.getItems().setAll(PreconisationDB.getToutesLesPreconisations());
                });
            }
        });

        HBox actionsRegles = new HBox(10, btnModifier, btnSupprimer);

        HBox form = new HBox(10);
        TextField txtOp = new TextField(); txtOp.setPromptText("Opération (ex: Vidange)");
        TextField txtKm = new TextField(); txtKm.setPromptText("Km (ex: 20000)"); txtKm.setPrefWidth(120);
        TextField txtMois = new TextField(); txtMois.setPromptText("Mois (ex: 12)"); txtMois.setPrefWidth(120);

        Button btnAdd = new Button("+ Ajouter");
        btnAdd.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-cursor: hand;");
        btnAdd.setOnAction(e -> {
            try {
                Preconisation p = new Preconisation(0, txtOp.getText(), Integer.parseInt(txtKm.getText()), Integer.parseInt(txtMois.getText()));
                PreconisationDB.ajouterPreconisation(p);
                listePrecoUI.getItems().setAll(PreconisationDB.getToutesLesPreconisations());
                txtOp.clear(); txtKm.clear(); txtMois.clear();
            } catch (NumberFormatException ex) {
                System.out.println("Erreur de saisie.");
            }
        });

        form.getChildren().addAll(txtOp, txtKm, txtMois, btnAdd);
        vbox.getChildren().addAll(new Label("Règles actuelles :"), listePrecoUI, actionsRegles, new Label("Nouvelle règle :"), form);

        dialog.getDialogPane().setContent(vbox);
        dialog.showAndWait();
    }
}