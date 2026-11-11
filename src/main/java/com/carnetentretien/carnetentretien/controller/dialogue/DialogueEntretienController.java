package com.carnetentretien.carnetentretien.controller.dialogue;

import com.carnetentretien.carnetentretien.controller.DashboardContext;
import com.carnetentretien.carnetentretien.controller.ui.DialogueSupport;
import com.carnetentretien.carnetentretien.controller.ui.Formats;
import com.carnetentretien.carnetentretien.db.PreconisationDB;
import com.carnetentretien.carnetentretien.model.Entretien;
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
import java.util.Locale;
import java.util.Optional;

/**
 * Contrôleur de la fenêtre « J'ai fait un entretien » ({@code dialogues/entretien.fxml}).
 * Le formulaire est décrit dans le FXML ; ce contrôleur remplit les valeurs et vérifie la saisie.
 */
public class DialogueEntretienController {

    private static final String FXML = "/com/carnetentretien/carnetentretien/dialogues/entretien.fxml";

    @FXML private ComboBox<String> type;
    @FXML private DatePicker date;
    @FXML private TextField kmField;
    @FXML private TextField coutField;

    private Vehicule vehicule;
    private Entretien existant;
    private Entretien resultat;

    /**
     * Ouvre la fenêtre de saisie d'un entretien.
     *
     * @param dashboard services de la fenêtre principale
     * @param vehicule  véhicule concerné
     * @param existant  entretien à modifier, ou {@code null} pour une création
     * @return l'entretien saisi, ou vide si l'utilisateur a annulé
     */
    public static Optional<Entretien> ouvrir(DashboardContext dashboard, Vehicule vehicule, Entretien existant) {
        FXMLLoader loader = new FXMLLoader(DialogueEntretienController.class.getResource(FXML));
        VBox contenu;
        try {
            contenu = loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        DialogueEntretienController controleur = loader.getController();
        controleur.initialiser(vehicule, existant);

        String titre = existant == null ? "J'ai fait un entretien" : "Modifier l'entretien";
        boolean ok = DialogueSupport.afficherFormulaire(dashboard, titre, contenu, "✔  Enregistrer", controleur::valider);
        return ok ? Optional.ofNullable(controleur.resultat) : Optional.empty();
    }

    private void initialiser(Vehicule vehicule, Entretien existant) {
        this.vehicule = vehicule;
        this.existant = existant;

        for (Preconisation p : PreconisationDB.getToutesLesPreconisations()) {
            type.getItems().add(p.getTypeOperation());
        }
        if (existant != null) type.setValue(existant.getTypeOperation());

        date.setValue(existant != null ? existant.getDateRealisation() : LocalDate.now());
        kmField.setText(String.valueOf(existant != null ? existant.getKilometrageRealisation()
                : vehicule.getKilometrageActuel()));
        if (existant != null) coutField.setText(String.format(Locale.FRANCE, "%.2f", existant.getCout()));
    }

    private String valider() {
        String nom = type.getEditor().getText();
        if (nom == null || nom.isBlank()) return "Veuillez indiquer ce qui a été fait (exemple : Vidange).";
        if (date.getValue() == null) return "Veuillez choisir la date de l'entretien.";
        if (date.getValue().isAfter(LocalDate.now())) return "La date ne peut pas être dans le futur.";
        if (!Formats.estEntier(kmField.getText())) return "Le kilométrage doit être un nombre, par exemple 45000.";
        if (!Formats.estMontant(coutField.getText())) return "Le prix doit être un nombre, par exemple 89,90.";

        resultat = new Entretien(existant != null ? existant.getId() : 0, vehicule.getId(),
                nom.trim(), date.getValue(),
                Formats.parseEntier(kmField.getText()), Formats.parseMontant(coutField.getText()));
        return null;
    }
}
