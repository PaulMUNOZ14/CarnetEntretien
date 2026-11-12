package com.carnetentretien.carnetentretien.controller.dialogue;

import com.carnetentretien.carnetentretien.controller.DashboardContext;
import com.carnetentretien.carnetentretien.controller.ui.DialogueSupport;
import com.carnetentretien.carnetentretien.db.VehiculeDB;
import com.carnetentretien.carnetentretien.model.ControleTechniquePlanifie;
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
 * Contrôleur de la fenêtre « Planifier un contrôle technique »
 * ({@code dialogues/controle-technique-planification.fxml}). Le formulaire est décrit dans le FXML ;
 * ce contrôleur remplit les valeurs et vérifie la saisie.
 */
public class DialogueControleTechniquePlanificationController {

    private static final String FXML = "/com/carnetentretien/carnetentretien/dialogues/controle-technique-planification.fxml";

    @FXML private ComboBox<Vehicule> vehicule;
    @FXML private DatePicker date;
    @FXML private TextField note;

    private ControleTechniquePlanifie existant;
    private ControleTechniquePlanifie resultat;

    /**
     * Ouvre la fenêtre pour planifier un nouveau contrôle technique.
     *
     * @param dashboard    services de la fenêtre principale
     * @param suggere      véhicule présélectionné, ou {@code null}
     * @param dateSuggeree date présélectionnée (par exemple l'échéance calculée), ou {@code null}
     * @return la planification saisie, ou vide si l'utilisateur a annulé
     */
    public static Optional<ControleTechniquePlanifie> ouvrirCreation(DashboardContext dashboard, Vehicule suggere,
                                                                     LocalDate dateSuggeree) {
        return ouvrir(dashboard, null, suggere, dateSuggeree);
    }

    /**
     * Ouvre la fenêtre pour modifier une planification de contrôle technique existante.
     *
     * @param dashboard services de la fenêtre principale
     * @param existant  planification à modifier
     * @return la planification modifiée, ou vide si l'utilisateur a annulé
     */
    public static Optional<ControleTechniquePlanifie> ouvrirModification(DashboardContext dashboard,
                                                                          ControleTechniquePlanifie existant) {
        return ouvrir(dashboard, existant, null, null);
    }

    private static Optional<ControleTechniquePlanifie> ouvrir(DashboardContext dashboard,
                                                              ControleTechniquePlanifie existant,
                                                              Vehicule vehiculeSuggere,
                                                              LocalDate dateSuggeree) {
        FXMLLoader loader = new FXMLLoader(DialogueControleTechniquePlanificationController.class.getResource(FXML));
        VBox contenu;
        try {
            contenu = loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        DialogueControleTechniquePlanificationController controleur = loader.getController();
        controleur.initialiser(existant, vehiculeSuggere, dateSuggeree);

        String titre = existant == null ? "Planifier un contrôle technique" : "Modifier la planification du contrôle";
        boolean ok = DialogueSupport.afficherFormulaire(dashboard, titre, contenu, "✔  Enregistrer", controleur::valider);
        return ok ? Optional.ofNullable(controleur.resultat) : Optional.empty();
    }

    private void initialiser(ControleTechniquePlanifie existant, Vehicule vehiculeSuggere, LocalDate dateSuggeree) {
        this.existant = existant;

        List<Vehicule> tous = VehiculeDB.getTousLesVehicules();
        vehicule.getItems().setAll(tous);

        if (existant != null) {
            selectionnerVehicule(existant.getVehiculeId());
            date.setValue(existant.getDatePrevue());
            note.setText(existant.getNote() != null ? existant.getNote() : "");
        } else {
            if (vehiculeSuggere != null) {
                selectionnerVehicule(vehiculeSuggere.getId());
            } else if (tous.size() == 1) {
                vehicule.setValue(tous.get(0));
            }

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
        if (date.getValue() == null) return "Veuillez choisir la date prévue.";
        if (date.getValue().isBefore(LocalDate.now())) return "La date prévue ne peut pas être dans le passé.";

        String remarque = note.getText() == null ? "" : note.getText().trim();
        resultat = new ControleTechniquePlanifie(
                existant != null ? existant.getId() : 0,
                vehicule.getValue().getId(),
                date.getValue(),
                remarque.isEmpty() ? null : remarque);
        return null;
    }
}
