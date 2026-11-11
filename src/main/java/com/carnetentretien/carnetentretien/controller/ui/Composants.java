package com.carnetentretien.carnetentretien.controller.ui;

import com.carnetentretien.carnetentretien.model.Vehicule;
import com.carnetentretien.carnetentretien.service.ImageService;
import com.carnetentretien.carnetentretien.service.RecherchePlaqueService;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/** Fabrication des petits éléments graphiques réutilisés par les écrans (photo, plaque, bouton). */
public final class Composants {

    private Composants() {
    }

    /** @return l'adresse de la photo à afficher : celle choisie par l'utilisateur, sinon celle trouvée via la plaque, sinon {@code null} */
    public static String sourceImage(Vehicule v) {
        String locale = ImageService.adresse(v.getImageLocale());
        return locale != null ? locale : v.getImageUrl();
    }

    /** Cadre avec la photo du véhicule ; affiche 🚗 tant que la photo n'est pas chargée (ou s'il n'y en a pas). */
    public static StackPane creerVignette(String url, double largeur, double hauteur) {
        Label repli = new Label("🚗");
        repli.getStyleClass().add("carte-icone");
        ImageView vue = new ImageView();
        vue.setFitWidth(largeur - 10);
        vue.setFitHeight(hauteur - 10);
        vue.setPreserveRatio(true);
        vue.setVisible(false);

        StackPane cadre = new StackPane(repli, vue);
        cadre.getStyleClass().add("photo-cadre");
        cadre.setMinSize(largeur, hauteur);
        cadre.setPrefSize(largeur, hauteur);
        cadre.setMaxSize(largeur, hauteur);
        chargerImage(vue, repli, url);
        return cadre;
    }

    /**
     * Charge une image en arrière-plan ; tant qu'elle n'est pas prête (ou en cas d'échec) l'icône 🚗 reste affichée.
     * Une recherche plus récente sur la même vue annule la précédente.
     */
    public static void chargerImage(ImageView vue, Node repli, String url) {
        vue.setUserData(url);
        vue.setImage(null);
        vue.setVisible(false);
        repli.setVisible(true);
        if (url == null || url.isBlank()) return;

        final Image image;
        try {
            image = new Image(url, 520, 320, true, true, true);
        } catch (IllegalArgumentException e) {
            return;   // lien invalide
        }
        Runnable verifier = () -> {
            if (!url.equals(vue.getUserData())) return;   // une recherche plus récente a pris le relais
            if (image.isError()) {
                vue.setVisible(false);
                repli.setVisible(true);
            } else if (image.getProgress() >= 1.0) {
                vue.setImage(image);
                vue.setVisible(true);
                repli.setVisible(false);
            }
        };
        image.progressProperty().addListener((o, a, b) -> verifier.run());
        image.errorProperty().addListener((o, a, b) -> verifier.run());
        verifier.run();
    }

    /** Plaque façon française : bande bleue « F » + numéro. */
    public static HBox creerPlaque(String texte) {
        Label bande = new Label("F");
        bande.getStyleClass().add("plaque-bande");
        bande.setMaxHeight(Double.MAX_VALUE);
        Label numero = new Label(RecherchePlaqueService.formater(texte));
        numero.getStyleClass().add("plaque-texte");
        HBox plaque = new HBox(bande, numero);
        plaque.getStyleClass().add("plaque");
        plaque.setAlignment(Pos.CENTER_LEFT);
        plaque.setMaxWidth(Region.USE_PREF_SIZE);
        return plaque;
    }

    /** Crée un bouton avec la classe commune {@code btn} et les classes supplémentaires fournies. */
    public static Button bouton(String texte, String... classes) {
        Button b = new Button(texte);
        b.getStyleClass().add("btn");
        b.getStyleClass().addAll(classes);
        return b;
    }

    /** Affiche ou masque un composant <b>et</b> libère sa place dans la mise en page. */
    public static void montrer(Node n, boolean visible) {
        n.setVisible(visible);
        n.setManaged(visible);
    }
}
