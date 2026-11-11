package com.carnetentretien.carnetentretien.controller;

import com.carnetentretien.carnetentretien.controller.dialogue.DialogueCleApiController;
import com.carnetentretien.carnetentretien.controller.vue.AccueilController;
import com.carnetentretien.carnetentretien.controller.vue.VehiculeController;
import com.carnetentretien.carnetentretien.db.PreconisationDB;
import com.carnetentretien.carnetentretien.model.Preconisation;
import com.carnetentretien.carnetentretien.model.Vehicule;
import com.carnetentretien.carnetentretien.service.ConfigService;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * Contrôleur de la fenêtre principale ({@code dashboard.fxml}).
 * <p>
 * Il ne dessine plus les écrans lui-même : il assemble deux vues FXML indépendantes
 * ({@code vues/accueil.fxml} et {@code vues/vehicule.fxml}) et leur fournit les services communs
 * (navigation, message de confirmation, adaptation à la taille de l'écran).
 */
public class DashboardController implements DashboardContext {

    /** Largeur de fenêtre (px) en dessous de laquelle le mode compact est activé. */
    private static final double SEUIL_COMPACT = 820;

    @FXML private StackPane racine;
    @FXML private VBox accueil;
    @FXML private AccueilController accueilController;
    @FXML private VBox vehicule;
    @FXML private VehiculeController vehiculeController;
    @FXML private Label toast;

    /** Véhicule actuellement ouvert ; {@code null} sur l'écran d'accueil. */
    private Vehicule vehiculeSelectionne = null;
    private SequentialTransition animationToast;

    /** {@code true} quand la fenêtre est étroite : polices et marges réduites (classe CSS {@code compact}). */
    private boolean compact = false;

    /**
     * Appelée par JavaFX après le chargement du FXML : branche les vues incluses, installe les règles
     * par défaut au premier lancement, affiche l'accueil et, si l'utilisateur n'a pas de clé API,
     * ouvre une seule fois la fenêtre d'aide.
     */
    @FXML
    public void initialize() {
        accueilController.installer(this);
        vehiculeController.installer(this);

        racine.widthProperty().addListener((o, a, b) -> adapterTaille(b.doubleValue()));
        adapterTaille(racine.getPrefWidth());

        installerReglesParDefaut();
        afficherAccueil();

        // Première utilisation sans clé personnelle : on explique une seule fois comment en obtenir une.
        if (!ConfigService.aCleApi() && !ConfigService.aideDejaAffichee()) {
            ConfigService.marquerAideAffichee();
            Platform.runLater(this::ouvrirCleApi);
        }
    }

    /** Active ou désactive le mode compact selon la largeur de la fenêtre. */
    private void adapterTaille(double largeur) {
        compact = largeur < SEUIL_COMPACT;
        if (compact) {
            if (!racine.getStyleClass().contains("compact")) racine.getStyleClass().add("compact");
        } else {
            racine.getStyleClass().remove("compact");
        }
    }

    /** Au tout premier lancement (aucune règle en base), ajoute des règles courantes pour que l'écran ne soit jamais vide. */
    private void installerReglesParDefaut() {
        if (!PreconisationDB.getToutesLesPreconisations().isEmpty()) return;

        Object[][] defauts = {
                {"Vidange", 10000, 12},
                {"Vidange boite de vitesse", 80000, 60},
                {"Filtre à air", 30000, 24},
                {"Filtre d'habitacle", 15000, 12},
                {"Plaquettes de frein", 40000, 48},
                {"Liquide de frein", 60000, 24},
                {"Pneus avant", 40000, 48},
                {"Pneus arrière", 40000, 48},
                {"Courroie de distribution", 120000, 72}
        };
        for (Object[] d : defauts) {
            PreconisationDB.ajouterPreconisation(
                    new Preconisation(0, (String) d[0], (Integer) d[1], (Integer) d[2]));
        }
    }

    /** Fenêtre d'aide expliquant comment obtenir la clé du service de plaques. */
    private void ouvrirCleApi() {
        DialogueCleApiController.ouvrir(this).ifPresent(this::toast);
    }

    // ---------- Navigation ----------

    @Override
    public void afficherAccueil() {
        vehiculeSelectionne = null;
        afficher(vehicule, false);
        afficher(accueil, true);
        accueilController.charger();
    }

    @Override
    public void ouvrirVehicule(Vehicule v) {
        vehiculeSelectionne = v;
        afficher(accueil, false);
        afficher(vehicule, true);
        vehiculeController.afficher(v);
    }

    @Override
    public void rafraichir() {
        if (vehiculeSelectionne != null) {
            vehiculeController.afficher(vehiculeSelectionne);
        } else {
            accueilController.charger();
        }
    }

    @Override
    public Vehicule vehiculeSelectionne() {
        return vehiculeSelectionne;
    }

    @Override
    public void definirVehiculeSelectionne(Vehicule v) {
        vehiculeSelectionne = v;
    }

    private void afficher(Node vue, boolean visible) {
        vue.setVisible(visible);
        vue.setManaged(visible);
    }

    // ---------- Services communs aux vues ----------

    /** Affiche un court message en bas de l'écran qui disparaît seul après quelques secondes. */
    @Override
    public void toast(String message) {
        if (animationToast != null) animationToast.stop();
        toast.setText(message);
        toast.setOpacity(1);
        toast.setVisible(true);

        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        FadeTransition fondu = new FadeTransition(Duration.millis(600), toast);
        fondu.setToValue(0);
        animationToast = new SequentialTransition(pause, fondu);
        animationToast.setOnFinished(e -> toast.setVisible(false));
        animationToast.play();
    }

    @Override
    public Window fenetreActive() {
        return Window.getWindows().stream()
                .filter(Window::isShowing)
                .filter(Window::isFocused)
                .findFirst()
                .orElse(racine.getScene().getWindow());
    }

    /** Zone utilisable (hors barre des tâches) de l'écran où se trouve la fenêtre. */
    @Override
    public Rectangle2D zoneEcran() {
        Window w = fenetreActive();
        return Screen.getScreensForRectangle(w.getX(), w.getY(), Math.max(1, w.getWidth()), Math.max(1, w.getHeight()))
                .stream().findFirst().orElse(Screen.getPrimary()).getVisualBounds();
    }

    /** Largeur d'une fenêtre de saisie, réduite si l'écran est petit. */
    @Override
    public double largeurDialogue(double voulue) {
        return Math.clamp(zoneEcran().getWidth() * 0.92, 280, voulue);
    }

    @Override
    public double hauteurMaxDialogue() {
        return Math.max(160, zoneEcran().getHeight() - 230);
    }

    @Override
    public boolean estCompact() {
        return compact;
    }

    @Override
    public boolean petitEcran() {
        Rectangle2D z = zoneEcran();
        return z.getWidth() < 1300 || z.getHeight() < 800;
    }
}
