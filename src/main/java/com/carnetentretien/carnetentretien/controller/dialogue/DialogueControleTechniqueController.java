package com.carnetentretien.carnetentretien.controller.dialogue;

import com.carnetentretien.carnetentretien.controller.DashboardContext;
import com.carnetentretien.carnetentretien.controller.ui.DialogueSupport;
import com.carnetentretien.carnetentretien.model.ControleTechnique;
import com.carnetentretien.carnetentretien.model.Vehicule;
import com.carnetentretien.carnetentretien.service.ControleTechniqueService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Contrôleur de la fenêtre « Enregistrer un contrôle technique »
 * ({@code dialogues/controle-technique.fxml}). Le formulaire est décrit dans le FXML ;
 * ce contrôleur propose la date du prochain contrôle et vérifie la saisie.
 */
public class DialogueControleTechniqueController {

    private static final String FXML = "/com/carnetentretien/carnetentretien/dialogues/controle-technique.fxml";

    @FXML private DatePicker dateRealisation;
    @FXML private ComboBox<ControleTechnique.Resultat> resultat;
    @FXML private DatePicker dateProchaine;
    @FXML private TextField note;

    private Vehicule vehicule;
    private ControleTechnique existant;
    private ControleTechnique resultatSaisi;

    /** Évite que le remplissage initial du formulaire écrase la date existante. */
    private boolean initialisation;

    /**
     * Ouvre la fenêtre pour enregistrer ou modifier un contrôle technique.
     *
     * @param dashboard services de la fenêtre principale
     * @param vehicule  véhicule concerné
     * @param existant  contrôle à modifier, ou {@code null} pour une création
     * @return le contrôle saisi, ou vide si l'utilisateur a annulé
     */
    public static Optional<ControleTechnique> ouvrir(DashboardContext dashboard, Vehicule vehicule,
                                                     ControleTechnique existant) {
        return ouvrir(dashboard, vehicule, existant, null);
    }

    /**
     * Ouvre la fenêtre pour enregistrer un contrôle technique en proposant une date de réalisation.
     *
     * @param dashboard            services de la fenêtre principale
     * @param vehicule             véhicule concerné
     * @param existant             contrôle à modifier, ou {@code null} pour une création
     * @param dateRealisationSuggeree date proposée lors d'une création (par exemple la date d'une
     *                                planification), ou {@code null}. Une date future est ignorée.
     * @return le contrôle saisi, ou vide si l'utilisateur a annulé
     */
    public static Optional<ControleTechnique> ouvrir(DashboardContext dashboard, Vehicule vehicule,
                                                     ControleTechnique existant, LocalDate dateRealisationSuggeree) {
        FXMLLoader loader = new FXMLLoader(DialogueControleTechniqueController.class.getResource(FXML));
        VBox contenu;
        try {
            contenu = loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        DialogueControleTechniqueController controleur = loader.getController();
        controleur.initialiser(vehicule, existant, dateRealisationSuggeree);

        String titre = existant == null ? "Enregistrer un contrôle technique" : "Modifier le contrôle technique";
        boolean ok = DialogueSupport.afficherFormulaire(dashboard, titre, contenu, "✔  Enregistrer", controleur::valider);
        return ok ? Optional.ofNullable(controleur.resultatSaisi) : Optional.empty();
    }

    private void initialiser(Vehicule vehicule, ControleTechnique existant, LocalDate dateRealisationSuggeree) {
        this.vehicule = vehicule;
        this.existant = existant;

        resultat.getItems().setAll(ControleTechnique.Resultat.values());
        initialisation = true;
        if (existant != null) {
            dateRealisation.setValue(existant.getDateRealisation());
            resultat.setValue(existant.getResultat());
            dateProchaine.setValue(existant.getDateProchaine());
            note.setText(existant.getNote() != null ? existant.getNote() : "");
        } else {
            LocalDate aujourdhui = LocalDate.now();
            LocalDate realisation = dateRealisationSuggeree != null && !dateRealisationSuggeree.isAfter(aujourdhui)
                    ? dateRealisationSuggeree : aujourdhui;
            dateRealisation.setValue(realisation);
            resultat.setValue(ControleTechnique.Resultat.FAVORABLE);
            dateProchaine.setValue(ControleTechniqueService.prochaineDate(
                    realisation, ControleTechnique.Resultat.FAVORABLE));
        }
        initialisation = false;

        dateRealisation.valueProperty().addListener((o, ancien, nouveau) -> recalculer());
        resultat.valueProperty().addListener((o, ancien, nouveau) -> recalculer());
    }

    /** Propose la date du prochain contrôle en suivant le résultat choisi. */
    private void recalculer() {
        if (initialisation) return;
        LocalDate realisation = dateRealisation.getValue();
        ControleTechnique.Resultat choix = resultat.getValue();
        if (realisation != null && choix != null) {
            dateProchaine.setValue(ControleTechniqueService.prochaineDate(realisation, choix));
        }
    }

    private String valider() {
        if (vehicule == null) return "Aucun véhicule n'est sélectionné.";
        if (dateRealisation.getValue() == null) return "Veuillez indiquer la date du contrôle technique.";
        if (dateRealisation.getValue().isAfter(LocalDate.now())) {
            return "La date du contrôle technique ne peut pas être dans le futur.";
        }
        if (resultat.getValue() == null) return "Veuillez choisir le résultat du contrôle technique.";
        if (dateProchaine.getValue() == null) return "Veuillez indiquer la date du prochain contrôle.";
        if (!dateProchaine.getValue().isAfter(dateRealisation.getValue())) {
            return "La date du prochain contrôle doit être postérieure à la date du contrôle.";
        }

        String remarque = note.getText() == null ? "" : note.getText().trim();
        resultatSaisi = new ControleTechnique(
                existant != null ? existant.getId() : 0,
                vehicule.getId(),
                dateRealisation.getValue(),
                resultat.getValue(),
                dateProchaine.getValue(),
                remarque.isEmpty() ? null : remarque);
        return null;
    }
}
