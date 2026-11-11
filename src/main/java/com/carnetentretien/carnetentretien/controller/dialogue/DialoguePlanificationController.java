package com.carnetentretien.carnetentretien.controller.dialogue;

import com.carnetentretien.carnetentretien.controller.DashboardContext;
import com.carnetentretien.carnetentretien.controller.ui.DialogueSupport;
import com.carnetentretien.carnetentretien.db.PreconisationDB;
import com.carnetentretien.carnetentretien.db.VehiculeDB;
import com.carnetentretien.carnetentretien.model.EntretienPlanifie;
import com.carnetentretien.carnetentretien.model.Preconisation;
import com.carnetentretien.carnetentretien.model.Vehicule;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Contrôleur de la fenêtre « Planifier un entretien » ({@code dialogues/planification.fxml}).
 * Le formulaire est décrit dans le FXML ; ce contrôleur remplit les valeurs et vérifie la saisie.
 */
public class DialoguePlanificationController {

    private static final String FXML = "/com/carnetentretien/carnetentretien/dialogues/planification.fxml";

    @FXML private ComboBox<Vehicule> vehicule;
    @FXML private ComboBox<String> type;
    @FXML private DatePicker date;
    @FXML private TextField note;

    private EntretienPlanifie existant;
    private EntretienPlanifie resultat;

    /**
     * Ouvre la fenêtre pour planifier un nouvel entretien.
     *
     * @param dashboard       services de la fenêtre principale
     * @param vehiculeSuggere véhicule présélectionné, ou {@code null}
     * @param typeSuggere     opération présélectionnée (ex. une échéance), ou {@code null}
     * @param dateSuggeree    date présélectionnée (ex. une échéance), ou {@code null}
     * @return la planification saisie, ou vide si l'utilisateur a annulé
     */
    public static Optional<EntretienPlanifie> ouvrirCreation(DashboardContext dashboard, Vehicule vehiculeSuggere,
                                                             String typeSuggere, LocalDate dateSuggeree) {
        return ouvrir(dashboard, null, vehiculeSuggere, typeSuggere, dateSuggeree);
    }

    /**
     * Ouvre la fenêtre pour modifier une planification existante.
     *
     * @param dashboard services de la fenêtre principale
     * @param existant  planification à modifier
     * @return la planification modifiée, ou vide si l'utilisateur a annulé
     */
    public static Optional<EntretienPlanifie> ouvrirModification(DashboardContext dashboard, EntretienPlanifie existant) {
        return ouvrir(dashboard, existant, null, null, null);
    }

    private static Optional<EntretienPlanifie> ouvrir(DashboardContext dashboard, EntretienPlanifie existant,
                                                      Vehicule vehiculeSuggere, String typeSuggere, LocalDate dateSuggeree) {
        FXMLLoader loader = new FXMLLoader(DialoguePlanificationController.class.getResource(FXML));
        VBox contenu;
        try {
            contenu = loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        DialoguePlanificationController controleur = loader.getController();
        controleur.initialiser(existant, vehiculeSuggere, typeSuggere, dateSuggeree);

        String titre = existant == null ? "Planifier un entretien" : "Modifier la planification";
        boolean ok = DialogueSupport.afficherFormulaire(dashboard, titre, contenu, "✔  Enregistrer", controleur::valider);
        return ok ? Optional.ofNullable(controleur.resultat) : Optional.empty();
    }

    private void initialiser(EntretienPlanifie existant, Vehicule vehiculeSuggere,
                             String typeSuggere, LocalDate dateSuggeree) {
        this.existant = existant;

        List<Vehicule> tous = VehiculeDB.getTousLesVehicules();
        vehicule.getItems().setAll(tous);
        for (Preconisation p : PreconisationDB.getToutesLesPreconisations()) {
            type.getItems().add(p.getTypeOperation());
        }

        if (existant != null) {
            selectionnerVehicule(existant.getVehiculeId());
            type.setValue(existant.getTypeOperation());
            date.setValue(existant.getDatePrevue());
            note.setText(existant.getNote() != null ? existant.getNote() : "");
        } else {
            if (vehiculeSuggere != null) {
                selectionnerVehicule(vehiculeSuggere.getId());
            } else if (tous.size() == 1) {
                vehicule.setValue(tous.get(0));
            }
            if (typeSuggere != null) type.setValue(typeSuggere);

            LocalDate aujourdhui = LocalDate.now();
            date.setValue(dateSuggeree != null && !dateSuggeree.isBefore(aujourdhui) ? dateSuggeree : aujourdhui);
        }
    }

    private void selectionnerVehicule(int vehiculeId) {
        for (Vehicule v : vehicule.getItems()) {
            if (v.getId() == vehiculeId) {
                vehicule.setValue(v);
                return;
            }
        }
    }

    private String valider() {
        if (vehicule.getValue() == null) return "Veuillez choisir le véhicule concerné.";
        String nom = type.getEditor().getText();
        if (nom == null || nom.isBlank()) return "Veuillez indiquer l'entretien à planifier (exemple : Vidange).";
        if (date.getValue() == null) return "Veuillez choisir la date prévue.";
        if (date.getValue().isBefore(LocalDate.now())) return "La date prévue ne peut pas être dans le passé.";

        String remarque = note.getText() == null ? "" : note.getText().trim();
        resultat = new EntretienPlanifie(
                existant != null ? existant.getId() : 0,
                vehicule.getValue().getId(),
                nom.trim(),
                date.getValue(),
                remarque.isEmpty() ? null : remarque);
        return null;
    }
}
