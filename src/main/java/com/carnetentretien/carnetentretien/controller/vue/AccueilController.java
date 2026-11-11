package com.carnetentretien.carnetentretien.controller.vue;

import com.carnetentretien.carnetentretien.controller.DashboardContext;
import com.carnetentretien.carnetentretien.controller.dialogue.DialogueReglagesController;
import com.carnetentretien.carnetentretien.controller.dialogue.DialogueVehiculeController;
import com.carnetentretien.carnetentretien.controller.ui.Composants;
import com.carnetentretien.carnetentretien.controller.ui.EntretienUi;
import com.carnetentretien.carnetentretien.controller.ui.Formats;
import com.carnetentretien.carnetentretien.db.VehiculeDB;
import com.carnetentretien.carnetentretien.model.Vehicule;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Contrôleur de l'écran d'accueil ({@code vues/accueil.fxml}) : la liste des véhicules
 * et le bouton d'ajout.
 */
public class AccueilController {

    @FXML private VBox blocBienvenue;
    @FXML private FlowPane conteneurVehicules;

    private DashboardContext dashboard;

    /** Renseigné par {@link com.carnetentretien.carnetentretien.controller.DashboardController} après le chargement du FXML. */
    public void installer(DashboardContext dashboard) {
        this.dashboard = dashboard;
    }

    /** Recharge la liste des véhicules depuis la base. */
    public void charger() {
        List<Vehicule> vehicules = VehiculeDB.getTousLesVehicules();
        conteneurVehicules.getChildren().clear();
        Composants.montrer(blocBienvenue, vehicules.isEmpty());

        for (Vehicule v : vehicules) {
            conteneurVehicules.getChildren().add(creerCarteVehicule(v));
        }
    }

    private Button creerCarteVehicule(Vehicule v) {
        StackPane photo = Composants.creerVignette(Composants.sourceImage(v), 260, 130);

        Label nom = new Label(v.getMarque() + " " + v.getModele());
        nom.getStyleClass().add("carte-titre");
        nom.setWrapText(true);
        nom.setAlignment(Pos.CENTER);
        nom.setMaxWidth(260);

        Label compteur = new Label(Formats.km(v.getKilometrageActuel()) + " km");
        compteur.getStyleClass().add("carte-km");

        Label etat = EntretienUi.creerBadge(EntretienUi.niveauGlobal(v));

        VBox contenu = new VBox(8, photo, nom);
        if (v.getImmatriculation() != null && !v.getImmatriculation().isBlank()) {
            contenu.getChildren().add(Composants.creerPlaque(v.getImmatriculation()));
        }
        contenu.getChildren().addAll(compteur, etat);
        contenu.setAlignment(Pos.CENTER);

        Button carte = new Button();
        carte.setGraphic(contenu);
        carte.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        carte.getStyleClass().add("carte-vehicule");
        carte.setOnAction(e -> dashboard.ouvrirVehicule(v));
        return carte;
    }

    /** Ouvre la saisie d'un nouveau véhicule et l'enregistre si l'utilisateur valide. */
    @FXML
    public void ajouterVehicule() {
        DialogueVehiculeController.ouvrir(dashboard, null).ifPresent(nouveau -> {
            VehiculeDB.ajouterVehicule(nouveau);
            charger();
            dashboard.toast("✔  Véhicule enregistré");
        });
    }

    /** Ouvre la fenêtre de gestion des règles de suivi, puis rafraîchit l'écran. */
    @FXML
    public void ouvrirReglages() {
        DialogueReglagesController.ouvrir(dashboard);
        dashboard.rafraichir();
    }

    /** Ouvre le calendrier de tous les véhicules. */
    @FXML
    public void ouvrirCalendrier() {
        dashboard.ouvrirCalendrier();
    }
}
