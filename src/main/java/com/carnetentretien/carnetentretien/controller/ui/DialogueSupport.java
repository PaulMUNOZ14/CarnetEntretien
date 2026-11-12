package com.carnetentretien.carnetentretien.controller.ui;

import com.carnetentretien.carnetentretien.controller.DashboardContext;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.function.Supplier;

/**
 * Outils communs aux fenêtres de saisie : habillage, validation et confirmation.
 * <p>
 * Les formulaires eux-mêmes sont décrits dans les fichiers FXML du dossier {@code dialogues/} ;
 * cette classe ne s'occupe que de la mécanique de la boîte de dialogue.
 */
public final class DialogueSupport {

    private static final String FEUILLE_STYLE = "/com/carnetentretien/carnetentretien/style.css";

    private DialogueSupport() {
    }

    /** Applique le style de l'application à une boîte de dialogue et la rattache à la fenêtre active. */
    public static void preparer(Dialog<?> dialog, String titre, DashboardContext contexte) {
        dialog.setTitle(titre);
        dialog.setHeaderText(null);
        dialog.setGraphic(null);
        dialog.getDialogPane().getStylesheets().add(
                DialogueSupport.class.getResource(FEUILLE_STYLE).toExternalForm());
        if (contexte.estCompact() || contexte.petitEcran()) {
            dialog.getDialogPane().getStyleClass().add("compact");
        }
        dialog.initOwner(contexte.fenetreActive());
    }

    /**
     * Affiche un formulaire défilant avec les boutons « Enregistrer » et « Annuler ».
     * La fenêtre reste ouverte tant que {@code validation} renvoie un message d'erreur
     * (affiché en rouge dans la fenêtre).
     *
     * @param contexte   services de la fenêtre principale
     * @param titre      titre de la fenêtre
     * @param contenu    formulaire chargé depuis un FXML
     * @param texteOk    libellé du bouton de validation
     * @param validation renvoie {@code null} si les données sont valides, sinon le message à afficher
     * @return {@code true} si l'utilisateur a validé
     */
    public static boolean afficherFormulaire(DashboardContext contexte, String titre, VBox contenu,
                                             String texteOk, Supplier<String> validation) {
        Dialog<ButtonType> dialog = new Dialog<>();
        preparer(dialog, titre, contexte);

        ButtonType ok = new ButtonType(texteOk, ButtonBar.ButtonData.OK_DONE);
        ButtonType annuler = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(ok, annuler);

        Label erreur = new Label();
        erreur.setWrapText(true);
        erreur.setMaxWidth(Double.MAX_VALUE);
        erreur.getStyleClass().add("message-erreur");
        Composants.montrer(erreur, false);
        contenu.getChildren().add(erreur);

        dialog.getDialogPane().setContent(defiler(contenu, contexte));

        dialog.setOnShown(e -> ajusterFenetre(dialog.getDialogPane().getScene().getWindow(), contexte));

        Button boutonOk = (Button) dialog.getDialogPane().lookupButton(ok);
        boutonOk.getStyleClass().addAll("btn", "btn-succes");
        dialog.getDialogPane().lookupButton(annuler).getStyleClass().addAll("btn", "btn-secondaire");

        boutonOk.addEventFilter(ActionEvent.ACTION, ev -> {
            String message = validation.get();
            if (message != null) {
                erreur.setText("⚠  " + message);
                Composants.montrer(erreur, true);
                ev.consume();               // la fenêtre reste ouverte
                dialog.getDialogPane().getScene().getWindow().sizeToScene();
                ajusterFenetre(dialog.getDialogPane().getScene().getWindow(), contexte);
            }
        });

        return dialog.showAndWait().filter(r -> r == ok).isPresent();
    }

    /**
     * Affiche une fenêtre de consultation avec un simple bouton « Fermer ».
     * Utilisée pour montrer un contenu plus long sans le modifier (par exemple le détail d'une journée
     * du calendrier).
     *
     * @param contexte services de la fenêtre principale
     * @param titre    titre de la fenêtre
     * @param contenu  contenu à afficher, défilant si nécessaire
     */
    public static void afficherDetail(DashboardContext contexte, String titre, VBox contenu) {
        Dialog<ButtonType> dialog = new Dialog<>();
        preparer(dialog, titre, contexte);

        ButtonType fermer = new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(fermer);
        dialog.getDialogPane().setContent(defiler(contenu, contexte));

        dialog.setOnShown(e -> ajusterFenetre(dialog.getDialogPane().getScene().getWindow(), contexte));

        Button boutonFermer = (Button) dialog.getDialogPane().lookupButton(fermer);
        boutonFermer.getStyleClass().addAll("btn", "btn-secondaire");
        dialog.showAndWait();
    }

    /** Prépare un contenu défilant et de largeur adaptée à la fenêtre de saisie. */
    private static ScrollPane defiler(VBox contenu, DashboardContext contexte) {
        contenu.setPrefWidth(contexte.largeurDialogue(720));
        if (contexte.petitEcran()) {
            contenu.lookupAll(".intro-dialogue").forEach(n -> Composants.montrer(n, false));
        }

        ScrollPane defilement = new ScrollPane(contenu);
        defilement.setFitToWidth(true);
        defilement.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        defilement.getStyleClass().add("zone-defilement");
        defilement.setMinHeight(0);
        defilement.prefViewportHeightProperty().bind(
                Bindings.min(contenu.heightProperty(), contexte.hauteurMaxDialogue()));
        defilement.setMaxHeight(contexte.hauteurMaxDialogue());
        return defilement;
    }

    /** Fenêtre « Êtes-vous sûr ? » dont le bouton par défaut est « Non » pour éviter les suppressions par erreur. */
    public static boolean confirmer(DashboardContext contexte, String titre, String question, String texteOui) {
        ButtonType oui = new ButtonType(texteOui, ButtonBar.ButtonData.OK_DONE);
        ButtonType non = new ButtonType("Non, je garde", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert alerte = new Alert(Alert.AlertType.WARNING, question, oui, non);
        preparer(alerte, titre, contexte);
        alerte.getDialogPane().setMinWidth(contexte.largeurDialogue(540));

        Button boutonOui = (Button) alerte.getDialogPane().lookupButton(oui);
        Button boutonNon = (Button) alerte.getDialogPane().lookupButton(non);
        boutonOui.getStyleClass().addAll("btn", "btn-danger");
        boutonNon.getStyleClass().addAll("btn", "btn-succes");
        boutonOui.setDefaultButton(false);
        boutonNon.setDefaultButton(true);

        return alerte.showAndWait().filter(r -> r == oui).isPresent();
    }

    /** Met à jour un libellé d'état et redimensionne la fenêtre qui le contient. */
    public static void definirStatut(Label label, String texte, String classe, DashboardContext contexte) {
        label.getStyleClass().removeAll("apercu-ok", "apercu-erreur", "apercu-attente");
        label.getStyleClass().add(classe);
        label.setText(texte);
        if (label.getScene() != null && label.getScene().getWindow() != null) {
            label.getScene().getWindow().sizeToScene();
            ajusterFenetre(label.getScene().getWindow(), contexte);
        }
    }

    /** Ramène une fenêtre dans la zone visible de l'écran et la centre. */
    public static void ajusterFenetre(Window w, DashboardContext contexte) {
        Rectangle2D z = contexte.zoneEcran();
        double maxH = z.getHeight() - 20;
        double maxL = z.getWidth() - 20;
        if (w.getHeight() > maxH) w.setHeight(maxH);
        if (w.getWidth() > maxL) w.setWidth(maxL);
        w.setX(z.getMinX() + (z.getWidth() - w.getWidth()) / 2);
        w.setY(z.getMinY() + Math.max(0, (z.getHeight() - w.getHeight()) / 2));
    }
}
