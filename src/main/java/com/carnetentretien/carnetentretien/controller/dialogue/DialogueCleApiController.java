package com.carnetentretien.carnetentretien.controller.dialogue;

import com.carnetentretien.carnetentretien.HelloApplication;
import com.carnetentretien.carnetentretien.controller.DashboardContext;
import com.carnetentretien.carnetentretien.controller.ui.DialogueSupport;
import com.carnetentretien.carnetentretien.service.ConfigService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;

/**
 * Contrôleur de la fenêtre « Ma clé d'accès » ({@code dialogues/cle-api.fxml}).
 * Une case vide supprime la clé personnelle.
 */
public class DialogueCleApiController {

    private static final String FXML = "/com/carnetentretien/carnetentretien/dialogues/cle-api.fxml";

    @FXML private Label adresse;
    @FXML private TextField cle;
    @FXML private Label etat;

    /**
     * Ouvre la fenêtre d'aide et de saisie de la clé API.
     *
     * @param dashboard services de la fenêtre principale
     * @return le message à afficher après enregistrement, ou vide si l'utilisateur a annulé
     */
    public static Optional<String> ouvrir(DashboardContext dashboard) {
        FXMLLoader loader = new FXMLLoader(DialogueCleApiController.class.getResource(FXML));
        VBox contenu;
        try {
            contenu = loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        DialogueCleApiController controleur = loader.getController();
        controleur.initialiser();

        boolean ok = DialogueSupport.afficherFormulaire(dashboard, "Ma clé d'accès",
                contenu, "✔  Enregistrer", controleur::valider);
        if (!ok) return Optional.empty();

        ConfigService.setCleApi(controleur.cle.getText());
        return Optional.of(ConfigService.aCleApi()
                ? "✔  Clé enregistrée"
                : "Clé supprimée : la clé de démonstration sera utilisée");
    }

    private void initialiser() {
        adresse.setText(ConfigService.LIEN_OBTENIR_CLE);
        cle.setText(ConfigService.getCleApi().orElse(""));

        String etatCle = ConfigService.aCleApi()
                ? "Clé enregistrée : " + ConfigService.masquerCle(ConfigService.getCleApi().orElse(""))
                : "Pas encore de clé personnelle : l'application utilise une clé de démonstration, très limitée.";
        etat.setText(etatCle + "  Videz la case puis enregistrez pour la supprimer. "
                + "La clé reste uniquement sur cet ordinateur.");
    }

    @FXML
    private void ouvrirSite() {
        HelloApplication.ouvrirLien(ConfigService.LIEN_OBTENIR_CLE);
    }

    private String valider() {
        return cle.getText().isBlank() || cle.getText().trim().matches("\\S+")
                ? null
                : "La clé ne doit pas contenir d'espace. Copiez-la de nouveau depuis le site.";
    }
}
