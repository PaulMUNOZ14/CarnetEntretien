package com.carnetentretien.carnetentretien.controller.dialogue;

import com.carnetentretien.carnetentretien.controller.DashboardContext;
import com.carnetentretien.carnetentretien.controller.ui.DialogueSupport;
import com.carnetentretien.carnetentretien.controller.ui.Formats;
import com.carnetentretien.carnetentretien.model.Preconisation;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;

/**
 * Contrôleur de la fenêtre « Nouvel entretien à surveiller » ({@code dialogues/regle.fxml}).
 * Le formulaire est décrit dans le FXML ; ce contrôleur remplit les valeurs et vérifie la saisie.
 */
public class DialogueRegleController {

    private static final String FXML = "/com/carnetentretien/carnetentretien/dialogues/regle.fxml";

    @FXML private TextField nom;
    @FXML private TextField kmField;
    @FXML private TextField mois;

    private Preconisation existante;
    private Preconisation resultat;

    /**
     * Ouvre la fenêtre de saisie d'une règle de suivi.
     *
     * @param dashboard services de la fenêtre principale
     * @param existante règle à modifier, ou {@code null} pour une création
     * @return la règle saisie, ou vide si l'utilisateur a annulé
     */
    public static Optional<Preconisation> ouvrir(DashboardContext dashboard, Preconisation existante) {
        FXMLLoader loader = new FXMLLoader(DialogueRegleController.class.getResource(FXML));
        VBox contenu;
        try {
            contenu = loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        DialogueRegleController controleur = loader.getController();
        controleur.initialiser(existante);

        String titre = existante == null ? "Nouvel entretien à surveiller" : "Modifier la règle";
        boolean ok = DialogueSupport.afficherFormulaire(dashboard, titre, contenu, "✔  Enregistrer", controleur::valider);
        return ok ? Optional.ofNullable(controleur.resultat) : Optional.empty();
    }

    private void initialiser(Preconisation existante) {
        this.existante = existante;
        if (existante != null) {
            nom.setText(existante.getTypeOperation());
            kmField.setText(String.valueOf(existante.getIntervalleKm()));
            mois.setText(String.valueOf(existante.getIntervalleMois()));
        }
    }

    private String valider() {
        if (nom.getText().isBlank()) return "Veuillez donner un nom à l'entretien (exemple : Vidange).";
        if (!Formats.estEntier(kmField.getText()) || Formats.parseEntier(kmField.getText()) == 0)
            return "Les kilomètres doivent être un nombre supérieur à 0, par exemple 15000.";
        if (!Formats.estEntier(mois.getText()) || Formats.parseEntier(mois.getText()) == 0)
            return "Les mois doivent être un nombre supérieur à 0, par exemple 12.";

        resultat = new Preconisation(existante != null ? existante.getId() : 0, nom.getText().trim(),
                Formats.parseEntier(kmField.getText()), Formats.parseEntier(mois.getText()));
        return null;
    }
}
