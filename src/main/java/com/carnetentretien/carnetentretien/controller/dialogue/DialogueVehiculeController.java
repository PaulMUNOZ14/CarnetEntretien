package com.carnetentretien.carnetentretien.controller.dialogue;

import com.carnetentretien.carnetentretien.controller.DashboardContext;
import com.carnetentretien.carnetentretien.controller.ui.Composants;
import com.carnetentretien.carnetentretien.controller.ui.DialogueSupport;
import com.carnetentretien.carnetentretien.controller.ui.Formats;
import com.carnetentretien.carnetentretien.model.Vehicule;
import com.carnetentretien.carnetentretien.service.ImageService;
import com.carnetentretien.carnetentretien.service.RecherchePlaqueService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Contrôleur de la fenêtre d'ajout/modification d'un véhicule ({@code dialogues/vehicule.fxml}).
 * <p>
 * La recherche par plaque se fait hors du thread JavaFX ; la photo personnelle n'est copiée
 * dans l'application qu'au clic sur « Enregistrer ».
 */
public class DialogueVehiculeController {

    private static final String FXML = "/com/carnetentretien/carnetentretien/dialogues/vehicule.fxml";

    @FXML private TextField plaque;
    @FXML private TextField marque;
    @FXML private TextField modele;
    @FXML private TextField kmCompteur;
    @FXML private TextField kmAnnuel;
    @FXML private Label aideKmCompteur;
    @FXML private Button btnRecherche;
    @FXML private Button btnRetirerPhoto;
    @FXML private ImageView photo;
    @FXML private Label repli;
    @FXML private Label apercuTitre;
    @FXML private Label apercuDetail;
    @FXML private Label apercuStatut;

    private DashboardContext dashboard;
    private Vehicule existant;

    /** Photo trouvée via la plaque et plaque à laquelle elle correspond. */
    private String imageUrl;
    private String plaqueDeLaPhoto = "";

    /** Photo choisie sur l'ordinateur : le fichier n'est copié qu'au clic sur « Enregistrer ». */
    private Path photoChoisie;
    private boolean photoRetiree;

    private Vehicule resultat;

    /**
     * Ouvre la fenêtre de saisie d'un véhicule.
     *
     * @param dashboard services de la fenêtre principale
     * @param existant  véhicule à modifier, ou {@code null} pour une création
     * @return le véhicule saisi, ou vide si l'utilisateur a annulé
     */
    public static Optional<Vehicule> ouvrir(DashboardContext dashboard, Vehicule existant) {
        FXMLLoader loader = new FXMLLoader(DialogueVehiculeController.class.getResource(FXML));
        VBox contenu;
        try {
            contenu = loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        DialogueVehiculeController controleur = loader.getController();
        controleur.initialiser(dashboard, existant);

        String titre = existant == null ? "Ajouter un véhicule" : "Modifier le véhicule";
        boolean ok = DialogueSupport.afficherFormulaire(dashboard, titre, contenu, "✔  Enregistrer", controleur::valider);
        return ok ? Optional.ofNullable(controleur.resultat) : Optional.empty();
    }

    private void initialiser(DashboardContext dashboard, Vehicule existant) {
        this.dashboard = dashboard;
        this.existant = existant;

        plaque.setTextFormatter(new TextFormatter<String>(c -> {
            c.setText(c.getText().toUpperCase());
            return c;
        }));

        if (existant != null) {
            imageUrl = existant.getImageUrl();
            plaqueDeLaPhoto = RecherchePlaqueService.normaliser(existant.getImmatriculation());

            plaque.setText(existant.getImmatriculation() != null ? existant.getImmatriculation() : "");
            marque.setText(existant.getMarque());
            modele.setText(existant.getModele());
            kmCompteur.setText(String.valueOf(existant.getKilometrageActuel()));
            kmCompteur.setDisable(true);
            aideKmCompteur.setText("Le compteur se met à jour depuis l'historique kilométrique : "
                    + "utilisez le bouton « ➕ Enregistrer un relevé » sur l'écran du véhicule.");
            kmAnnuel.setText(String.valueOf(existant.getKilometrageAnnuelEstime()));

            apercuTitre.setText(existant.getMarque() + " " + existant.getModele());
            definirStatut("Pour changer la photo : modifiez la plaque puis appuyez sur « Rechercher », "
                    + "ou choisissez votre propre photo.", "apercu-attente");
            Composants.chargerImage(photo, repli, adressePhoto());
        } else {
            kmAnnuel.setText("12000");
            apercuTitre.setText("Votre véhicule");
            definirStatut("Écrivez la plaque puis appuyez sur « Rechercher » : la photo et le nom "
                    + "de votre voiture apparaîtront ici.", "apercu-attente");
        }
        majBoutonRetirer();
    }

    /** Recherche la plaque saisie et remplit l'aperçu (appel réseau dans un thread séparé). */
    @FXML
    private void rechercher() {
        String saisie = plaque.getText();
        if (saisie == null || saisie.isBlank()) {
            definirStatut("⚠  Écrivez d'abord la plaque de votre véhicule (exemple : AB-123-CD).", "apercu-erreur");
            plaque.requestFocus();
            return;
        }
        if (!RecherchePlaqueService.formatValide(saisie)) {
            definirStatut("⚠  Cette plaque ne ressemble pas à une plaque française. Exemple : AB-123-CD.", "apercu-erreur");
            return;
        }

        btnRecherche.setDisable(true);
        btnRecherche.setText("⏳  Recherche…");
        definirStatut("Recherche en cours, quelques secondes…", "apercu-attente");

        // L'appel réseau se fait HORS du thread de l'interface : la fenêtre ne se fige pas.
        Task<RecherchePlaqueService.Resultat> tache = new Task<>() {
            @Override
            protected RecherchePlaqueService.Resultat call() {
                return RecherchePlaqueService.rechercher(saisie);
            }
        };
        tache.setOnSucceeded(ev -> {
            btnRecherche.setDisable(false);
            btnRecherche.setText("🔍  Rechercher");
            RecherchePlaqueService.Resultat r = tache.getValue();
            if (r.trouve()) {
                plaque.setText(RecherchePlaqueService.formater(saisie));
                marque.setText(r.marque());
                modele.setText(r.modele());
                imageUrl = r.imageUrl();
                plaqueDeLaPhoto = RecherchePlaqueService.normaliser(saisie);

                apercuTitre.setText(r.marque() + " " + r.modele());
                List<String> details = new ArrayList<>();
                if (r.annee() != null) details.add("mise en circulation " + r.annee());
                if (r.energie() != null && !r.energie().isBlank()) details.add(r.energie());
                if (r.version() != null && !r.version().isBlank()) details.add(r.version());
                apercuDetail.setText(String.join("  ·  ", details));
                Composants.chargerImage(photo, repli, adressePhoto());   // une photo choisie reste prioritaire

                definirStatut("✔  Véhicule trouvé ! Vérifiez, puis complétez le kilométrage ci-dessous."
                        + (r.imageUrl() == null ? "  (Pas de photo disponible pour ce modèle.)" : ""), "apercu-ok");
            } else {
                definirStatut("⚠  " + r.messageErreur()
                        + "  Vous pouvez aussi écrire la marque et le modèle vous-même ci-dessous.", "apercu-erreur");
            }
        });
        tache.setOnFailed(ev -> {
            btnRecherche.setDisable(false);
            btnRecherche.setText("🔍  Rechercher");
            definirStatut("⚠  La recherche a échoué. Vérifiez votre connexion Internet, "
                    + "ou écrivez la marque et le modèle vous-même ci-dessous.", "apercu-erreur");
        });
        Thread fil = new Thread(tache, "recherche-plaque");
        fil.setDaemon(true);
        fil.start();
    }

    /** Choisit une photo personnelle sur l'ordinateur (copiée seulement à l'enregistrement). */
    @FXML
    private void choisirPhoto() {
        FileChooser choix = new FileChooser();
        choix.setTitle("Choisir la photo de votre véhicule");
        choix.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Images (PNG, JPG, GIF, BMP)", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
        java.io.File fichier = choix.showOpenDialog(dashboard.fenetreActive());
        if (fichier == null) return;
        try {
            ImageService.valider(fichier.toPath());
        } catch (IOException ex) {
            definirStatut("⚠  " + ex.getMessage(), "apercu-erreur");
            return;
        }
        photoChoisie = fichier.toPath();
        photoRetiree = false;
        Composants.chargerImage(photo, repli, adressePhoto());
        definirStatut("✔  Photo choisie. Elle sera copiée dans l'application quand vous appuierez sur "
                + "« Enregistrer » : vous pourrez ensuite supprimer le fichier d'origine sans rien perdre.", "apercu-ok");
        majBoutonRetirer();
    }

    /** Retire la photo personnelle (la photo trouvée via la plaque reprend sa place). */
    @FXML
    private void retirerPhoto() {
        photoChoisie = null;
        photoRetiree = true;
        Composants.chargerImage(photo, repli, adressePhoto());
        definirStatut("Votre photo sera retirée quand vous appuierez sur « Enregistrer ». "
                + (imageUrl != null ? "La photo trouvée avec la plaque sera affichée à la place."
                : "Une icône 🚗 sera affichée à la place."), "apercu-attente");
        majBoutonRetirer();
    }

    private String adressePhoto() {
        if (photoChoisie != null) return photoChoisie.toUri().toString();
        if (!photoRetiree && existant != null) {
            String locale = ImageService.adresse(existant.getImageLocale());
            if (locale != null) return locale;
        }
        return imageUrl;
    }

    private void majBoutonRetirer() {
        Composants.montrer(btnRetirerPhoto, photoChoisie != null
                || (!photoRetiree && existant != null && existant.getImageLocale() != null));
    }

    private void definirStatut(String texte, String classe) {
        DialogueSupport.definirStatut(apercuStatut, texte, classe, dashboard);
    }

    private String valider() {
        if (photoChoisie != null && !Files.isRegularFile(photoChoisie))
            return "La photo choisie n'existe plus à son emplacement d'origine. Choisissez-la de nouveau.";
        if (!plaque.getText().isBlank() && !RecherchePlaqueService.formatValide(plaque.getText()))
            return "La plaque n'est pas valide (exemple : AB-123-CD). Corrigez-la, ou videz la case si vous ne l'avez pas.";
        if (marque.getText().isBlank()) return "Veuillez écrire la marque de la voiture (exemple : Renault).";
        if (modele.getText().isBlank()) return "Veuillez écrire le modèle de la voiture (exemple : Clio).";
        if (!Formats.estEntier(kmCompteur.getText()))
            return "Le kilométrage du compteur doit être un nombre, par exemple 45000.";
        if (!Formats.estEntier(kmAnnuel.getText()) || Formats.parseEntier(kmAnnuel.getText()) == 0)
            return "Les kilomètres par an doivent être un nombre supérieur à 0, par exemple 12000.";

        String plaqueFinale = plaque.getText().isBlank() ? null : RecherchePlaqueService.formater(plaque.getText());
        // Si la plaque a changé depuis la recherche, la photo ne correspond plus : on ne la garde pas.
        String photoFinale = RecherchePlaqueService.normaliser(plaque.getText()).equals(plaqueDeLaPhoto)
                ? imageUrl : null;

        // Photo personnelle : copiée ICI (et seulement ici) dans le dossier de l'application.
        String imageLocale = existant != null ? existant.getImageLocale() : null;
        if (photoRetiree) imageLocale = null;
        if (photoChoisie != null) {
            try {
                imageLocale = ImageService.importer(photoChoisie);
            } catch (IOException ex) {
                dashboard.toast("⚠  La photo n'a pas pu être enregistrée : " + ex.getMessage());
            }
        }

        resultat = new Vehicule(existant != null ? existant.getId() : 0,
                plaqueFinale, marque.getText().trim(), modele.getText().trim(),
                Formats.parseEntier(kmCompteur.getText()), Formats.parseEntier(kmAnnuel.getText()),
                photoFinale, imageLocale);
        return null;
    }
}
