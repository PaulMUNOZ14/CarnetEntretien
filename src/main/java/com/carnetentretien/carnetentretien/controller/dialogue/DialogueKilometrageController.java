package com.carnetentretien.carnetentretien.controller.dialogue;

import com.carnetentretien.carnetentretien.controller.DashboardContext;
import com.carnetentretien.carnetentretien.controller.ui.DialogueSupport;
import com.carnetentretien.carnetentretien.controller.ui.Formats;
import com.carnetentretien.carnetentretien.db.ReleveKilometrageDB;
import com.carnetentretien.carnetentretien.model.ReleveKilometrage;
import com.carnetentretien.carnetentretien.model.Vehicule;
import com.carnetentretien.carnetentretien.service.HistoriqueKilometrageService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Contrôleur de la fenêtre de saisie d'un relevé kilométrique
 * ({@code dialogues/kilometrage.fxml}). Le même formulaire sert à créer et à modifier un relevé.
 */
public class DialogueKilometrageController {

    private static final String FXML = "/com/carnetentretien/carnetentretien/dialogues/kilometrage.fxml";

    @FXML private DatePicker dateReleve;
    @FXML private TextField champ;
    @FXML private TextField note;

    private Vehicule vehicule;
    private ReleveKilometrage existant;
    private ReleveKilometrage resultat;

    /**
     * Ouvre la fenêtre pour enregistrer un nouveau relevé, daté d'aujourd'hui par défaut.
     *
     * @param dashboard services de la fenêtre principale
     * @param vehicule  véhicule dont on relève le compteur
     * @return le relevé saisi, ou vide si l'utilisateur a annulé
     */
    public static Optional<ReleveKilometrage> ouvrir(DashboardContext dashboard, Vehicule vehicule) {
        return ouvrir(dashboard, vehicule, null);
    }

    /**
     * Ouvre la fenêtre pour enregistrer ou modifier un relevé kilométrique.
     *
     * @param dashboard services de la fenêtre principale
     * @param vehicule  véhicule concerné
     * @param existant  relevé à modifier, ou {@code null} pour une création
     * @return le relevé saisi, ou vide si l'utilisateur a annulé
     */
    public static Optional<ReleveKilometrage> ouvrir(DashboardContext dashboard, Vehicule vehicule,
                                                     ReleveKilometrage existant) {
        FXMLLoader loader = new FXMLLoader(DialogueKilometrageController.class.getResource(FXML));
        VBox contenu;
        try {
            contenu = loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        DialogueKilometrageController controleur = loader.getController();
        controleur.initialiser(vehicule, existant);

        String titre = existant == null ? "Enregistrer un relevé kilométrique"
                : "Modifier le relevé kilométrique";
        boolean ok = DialogueSupport.afficherFormulaire(dashboard, titre, contenu, "✔  Enregistrer",
                controleur::valider);
        return ok ? Optional.ofNullable(controleur.resultat) : Optional.empty();
    }

    private void initialiser(Vehicule vehicule, ReleveKilometrage existant) {
        this.vehicule = vehicule;
        this.existant = existant;

        dateReleve.setValue(existant != null ? existant.getDateReleve() : LocalDate.now());
        champ.setText(String.valueOf(existant != null
                ? existant.getKilometrage() : vehicule.getKilometrageActuel()));
        if (existant != null && existant.getNote() != null) {
            note.setText(existant.getNote());
        }
    }

    private String valider() {
        if (vehicule == null) return "Aucun véhicule n'est sélectionné.";
        if (dateReleve.getValue() == null) return "Veuillez indiquer la date du relevé.";
        if (!Formats.estEntier(champ.getText())) {
            return "Le kilométrage doit être un nombre, par exemple 45000.";
        }

        String remarque = note.getText() == null ? "" : note.getText().trim();
        ReleveKilometrage candidat = new ReleveKilometrage(
                existant != null ? existant.getId() : 0,
                vehicule.getId(),
                dateReleve.getValue(),
                Formats.parseEntier(champ.getText()),
                remarque.isEmpty() ? null : remarque);

        String erreur = HistoriqueKilometrageService.verifier(
                ReleveKilometrageDB.getRelevesPourVehicule(vehicule.getId()),
                candidat, LocalDate.now());
        if (erreur != null) return erreur;

        resultat = candidat;
        return null;
    }
}
