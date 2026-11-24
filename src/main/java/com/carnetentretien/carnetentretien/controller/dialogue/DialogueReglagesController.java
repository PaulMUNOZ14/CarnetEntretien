package com.carnetentretien.carnetentretien.controller.dialogue;

import com.carnetentretien.carnetentretien.controller.DashboardContext;
import com.carnetentretien.carnetentretien.controller.ui.Composants;
import com.carnetentretien.carnetentretien.controller.ui.DialogueSupport;
import com.carnetentretien.carnetentretien.controller.ui.Formats;
import com.carnetentretien.carnetentretien.db.PreconisationDB;
import com.carnetentretien.carnetentretien.model.Preconisation;
import com.carnetentretien.carnetentretien.service.ConfigService;
import com.carnetentretien.carnetentretien.service.NotificationService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Contrôleur de la fenêtre « Réglages » ({@code dialogues/reglages.fxml}) :
 * liste des règles de suivi (ajout, modification, suppression) et activation des
 * notifications Windows.
 */
public class DialogueReglagesController {

    private static final String FXML = "/com/carnetentretien/carnetentretien/dialogues/reglages.fxml";

    @FXML private ScrollPane defilement;
    @FXML private VBox liste;
    @FXML private CheckBox notifications;
    @FXML private Button boutonTester;
    @FXML private Label etatNotifications;

    private DashboardContext dashboard;

    /** Ouvre la fenêtre de réglages. */
    public static void ouvrir(DashboardContext dashboard) {
        FXMLLoader loader = new FXMLLoader(DialogueReglagesController.class.getResource(FXML));
        VBox contenu;
        try {
            contenu = loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        DialogueReglagesController controleur = loader.getController();
        controleur.initialiser(dashboard);

        Dialog<ButtonType> dialog = new Dialog<>();
        DialogueSupport.preparer(dialog, "Réglages", dashboard);

        ButtonType fermer = new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(fermer);
        dialog.getDialogPane().lookupButton(fermer).getStyleClass().addAll("btn", "btn-primaire");

        contenu.setPrefWidth(dashboard.largeurDialogue(660));
        dialog.getDialogPane().setContent(contenu);
        dialog.showAndWait();
    }

    private void initialiser(DashboardContext dashboard) {
        this.dashboard = dashboard;
        defilement.setPrefHeight(Math.min(300, Math.max(100, dashboard.fenetreActive().getHeight() - 540)));
        remplir();
        initialiserNotifications();
    }

    // ---------- Notifications Windows ----------

    private void initialiserNotifications() {
        boolean supportees = NotificationService.estSupportee();
        notifications.setDisable(!supportees);
        boutonTester.setDisable(!supportees);
        notifications.setSelected(supportees && ConfigService.notificationsActives());
        majEtatNotifications();
    }

    /** Enregistre le choix de l'utilisateur et installe ou retire l'icône de notification. */
    @FXML
    private void changerNotifications() {
        ConfigService.setNotificationsActives(notifications.isSelected());
        NotificationService.appliquerReglage();
        majEtatNotifications();
    }

    /** Active si besoin les notifications, puis affiche une notification de démonstration. */
    @FXML
    private void testerNotifications() {
        if (!notifications.isSelected()) {
            notifications.setSelected(true);
            ConfigService.setNotificationsActives(true);
            NotificationService.appliquerReglage();
        }
        if (!NotificationService.tester()) {
            etatNotifications.setText("Impossible d'afficher la notification sur ce système.");
            return;
        }
        majEtatNotifications();
    }

    private void majEtatNotifications() {
        if (!NotificationService.estSupportee()) {
            etatNotifications.setText("Les notifications ne sont pas disponibles sur ce système.");
        } else if (notifications.isSelected()) {
            etatNotifications.setText("Un rappel par jour au maximum.");
        } else {
            etatNotifications.setText("Les notifications sont désactivées.");
        }
    }

    @FXML
    private void ajouter() {
        DialogueRegleController.ouvrir(dashboard, null).ifPresent(p -> {
            PreconisationDB.ajouterPreconisation(p);
            remplir();
        });
    }

    private void remplir() {
        liste.getChildren().clear();
        List<Preconisation> regles = PreconisationDB.getToutesLesPreconisations();

        if (regles.isEmpty()) {
            Label vide = new Label("Aucun entretien surveillé. Appuyez sur le bouton vert ci-dessous pour en ajouter un.");
            vide.getStyleClass().add("historique-vide");
            vide.setWrapText(true);
            liste.getChildren().add(vide);
            return;
        }

        for (Preconisation p : regles) {
            Label nom = new Label(p.getTypeOperation());
            nom.getStyleClass().add("historique-titre");
            nom.setWrapText(true);
            Label freq = new Label("Tous les " + Formats.km(p.getIntervalleKm()) + " km ou tous les "
                    + p.getIntervalleMois() + " mois");
            freq.getStyleClass().add("historique-detail");
            VBox texte = new VBox(3, nom, freq);
            HBox.setHgrow(texte, Priority.ALWAYS);

            Button modifier = Composants.bouton("✏  Modifier", "btn-modif", "btn-petit");
            modifier.setOnAction(e -> DialogueRegleController.ouvrir(dashboard, p).ifPresent(modifiee -> {
                PreconisationDB.modifierPreconisation(modifiee);
                remplir();
            }));

            Button supprimer = Composants.bouton("🗑  Supprimer", "btn-danger", "btn-petit");
            supprimer.setOnAction(e -> {
                if (DialogueSupport.confirmer(dashboard, "Ne plus surveiller",
                        "Voulez-vous arrêter de surveiller « " + p.getTypeOperation() + " » ?", "Oui, arrêter")) {
                    PreconisationDB.supprimerPreconisation(p.getId());
                    remplir();
                }
            });

            HBox ligne = new HBox(14, texte, modifier, supprimer);
            ligne.setAlignment(Pos.CENTER_LEFT);
            ligne.getStyleClass().add("ligne-historique");
            liste.getChildren().add(ligne);
        }
    }
}
