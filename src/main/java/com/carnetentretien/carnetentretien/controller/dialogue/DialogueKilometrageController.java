package com.carnetentretien.carnetentretien.controller.dialogue;

import com.carnetentretien.carnetentretien.controller.DashboardContext;
import com.carnetentretien.carnetentretien.controller.ui.DialogueSupport;
import com.carnetentretien.carnetentretien.controller.ui.Formats;
import com.carnetentretien.carnetentretien.model.Vehicule;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;

/**
 * Contrôleur de la fenêtre « Mettre à jour le kilométrage » ({@code dialogues/kilometrage.fxml}).
 */
public class DialogueKilometrageController {

    private static final String FXML = "/com/carnetentretien/carnetentretien/dialogues/kilometrage.fxml";

    @FXML private TextField champ;

    private Vehicule vehicule;
    private Integer resultat;

    /**
     * Ouvre la fenêtre de mise à jour du compteur.
     *
     * @param dashboard services de la fenêtre principale
     * @param vehicule  véhicule dont on met à jour le kilométrage
     * @return le nouveau kilométrage, ou vide si l'utilisateur a annulé
     */
    public static Optional<Integer> ouvrir(DashboardContext dashboard, Vehicule vehicule) {
        FXMLLoader loader = new FXMLLoader(DialogueKilometrageController.class.getResource(FXML));
        VBox contenu;
        try {
            contenu = loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        DialogueKilometrageController controleur = loader.getController();
        controleur.initialiser(vehicule);

        boolean ok = DialogueSupport.afficherFormulaire(dashboard, "Mettre à jour le kilométrage",
                contenu, "✔  Enregistrer", controleur::valider);
        return ok ? Optional.ofNullable(controleur.resultat) : Optional.empty();
    }

    private void initialiser(Vehicule vehicule) {
        this.vehicule = vehicule;
        champ.setText(String.valueOf(vehicule.getKilometrageActuel()));
    }

    private String valider() {
        if (!Formats.estEntier(champ.getText())) return "Le kilométrage doit être un nombre, par exemple 45000.";
        int valeur = Formats.parseEntier(champ.getText());
        if (valeur < vehicule.getKilometrageActuel()) {
            return "Ce nombre est plus petit que l'ancien (" + Formats.km(vehicule.getKilometrageActuel())
                    + " km). Vérifiez votre compteur, ou utilisez « Modifier » pour corriger une erreur.";
        }
        resultat = valeur;
        return null;
    }
}
