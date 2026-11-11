package com.carnetentretien.carnetentretien.controller.vue;

import com.carnetentretien.carnetentretien.controller.DashboardContext;
import com.carnetentretien.carnetentretien.controller.dialogue.DialogueCleApiController;
import com.carnetentretien.carnetentretien.controller.dialogue.DialogueEntretienController;
import com.carnetentretien.carnetentretien.controller.dialogue.DialogueKilometrageController;
import com.carnetentretien.carnetentretien.controller.dialogue.DialogueReglagesController;
import com.carnetentretien.carnetentretien.controller.dialogue.DialogueVehiculeController;
import com.carnetentretien.carnetentretien.controller.ui.Composants;
import com.carnetentretien.carnetentretien.controller.ui.DialogueSupport;
import com.carnetentretien.carnetentretien.controller.ui.EntretienUi;
import com.carnetentretien.carnetentretien.controller.ui.Formats;
import com.carnetentretien.carnetentretien.db.EntretienDB;
import com.carnetentretien.carnetentretien.db.VehiculeDB;
import com.carnetentretien.carnetentretien.model.Entretien;
import com.carnetentretien.carnetentretien.model.Vehicule;
import com.carnetentretien.carnetentretien.service.CalculateurEntretien;
import com.carnetentretien.carnetentretien.service.ExportService;
import com.carnetentretien.carnetentretien.service.ImageService;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Contrôleur de l'écran d'un véhicule ({@code vues/vehicule.fxml}) : photo, plaque,
 * jauges d'usure et historique des entretiens.
 */
public class VehiculeController {

    @FXML private Label titreVehicule;
    @FXML private Label sousTitreVehicule;
    @FXML private ImageView photoVehicule;
    @FXML private Label iconeVehicule;
    @FXML private HBox zonePlaque;
    @FXML private HBox bandeauEtat;
    @FXML private Label messageBandeau;
    @FXML private VBox conteneurJauges;
    @FXML private VBox conteneurHistorique;
    @FXML private Label totalDepense;

    private DashboardContext dashboard;
    private Vehicule vehicule;

    /** Renseigné par {@link com.carnetentretien.carnetentretien.controller.DashboardController} après le chargement du FXML. */
    public void installer(DashboardContext dashboard) {
        this.dashboard = dashboard;
    }

    /** Affiche un véhicule : en-tête, jauges d'usure et historique. */
    public void afficher(Vehicule v) {
        this.vehicule = v;
        if (v == null) return;

        titreVehicule.setText(v.getMarque() + " " + v.getModele());
        Composants.chargerImage(photoVehicule, iconeVehicule, Composants.sourceImage(v));

        zonePlaque.getChildren().clear();
        if (v.getImmatriculation() != null && !v.getImmatriculation().isBlank()) {
            zonePlaque.getChildren().add(Composants.creerPlaque(v.getImmatriculation()));
        }
        Composants.montrer(zonePlaque, !zonePlaque.getChildren().isEmpty());
        sousTitreVehicule.setText("Compteur : " + Formats.km(v.getKilometrageActuel()) + " km   -   environ "
                + Formats.km(v.getKilometrageAnnuelEstime()) + " km par an");

        List<CalculateurEntretien.Bilan> bilans = EntretienUi.analyser(v);
        afficherBandeau(bilans);

        conteneurJauges.getChildren().clear();
        if (bilans.isEmpty()) {
            Label vide = new Label("Aucun entretien n'est surveillé pour le moment.\n"
                    + "Appuyez sur « ⚙ Réglages » en haut de l'écran pour en ajouter.");
            vide.getStyleClass().add("historique-vide");
            vide.setWrapText(true);
            conteneurJauges.getChildren().add(vide);
        } else {
            for (CalculateurEntretien.Bilan b : bilans) {
                conteneurJauges.getChildren().add(creerCarteJauge(b));
            }
        }

        afficherHistorique(v);
    }

    private void afficherBandeau(List<CalculateurEntretien.Bilan> bilans) {
        int niveau = EntretienUi.niveauGlobal(bilans);
        String message;
        switch (niveau) {
            case EntretienUi.URGENT -> {
                long n = bilans.stream().filter(b -> EntretienUi.niveauDe(b.pourcentage) == EntretienUi.URGENT).count();
                message = "⚠  " + (n > 1 ? n + " entretiens sont à faire maintenant" : "Un entretien est à faire maintenant");
            }
            case EntretienUi.PREVOIR -> message = "⏰  Pensez à prévoir un entretien bientôt";
            case EntretienUi.OK -> message = "✔  Tout va bien : rien à faire pour le moment";
            case EntretienUi.INCONNU -> message = "ℹ  Ajoutez vos entretiens déjà faits pour recevoir des conseils";
            default -> message = "ℹ  Aucun entretien à surveiller : ouvrez les « Réglages »";
        }
        messageBandeau.setText(message);
        bandeauEtat.getStyleClass().removeAll("bandeau-ok", "bandeau-prevoir", "bandeau-urgent", "bandeau-neutre");
        bandeauEtat.getStyleClass().add("bandeau-" + EntretienUi.suffixe(niveau));
    }

    private VBox creerCarteJauge(CalculateurEntretien.Bilan b) {
        int niveau = EntretienUi.niveauDe(b.pourcentage);

        Label nom = new Label(b.piece);
        nom.getStyleClass().add("jauge-titre");
        nom.setWrapText(true);

        Region espace = new Region();
        HBox.setHgrow(espace, Priority.ALWAYS);

        HBox haut = new HBox(12, nom, espace, EntretienUi.creerBadge(niveau));
        haut.setAlignment(Pos.CENTER_LEFT);

        ProgressBar barre = new ProgressBar(Math.max(0, Math.min(b.pourcentage / 100.0, 1.0)));
        barre.setMaxWidth(Double.MAX_VALUE);
        barre.getStyleClass().add("barre-" + EntretienUi.suffixe(niveau));

        String[] details = b.details.split("\n");
        Label prevision = new Label("📅  " + details[0]);
        prevision.getStyleClass().add("jauge-prevision");
        prevision.setWrapText(true);

        VBox carte = new VBox(10, haut, barre, prevision);
        if (details.length > 1) {
            Label historique = new Label(details[1]);
            historique.getStyleClass().add("jauge-historique");
            historique.setWrapText(true);
            carte.getChildren().add(historique);
        }
        carte.getStyleClass().addAll("carte-jauge", "jauge-" + EntretienUi.suffixe(niveau));
        return carte;
    }

    private void afficherHistorique(Vehicule v) {
        List<Entretien> liste = new ArrayList<>(EntretienDB.getEntretiensPourVehicule(v.getId()));
        liste.sort(Comparator.comparing(Entretien::getDateRealisation).reversed());

        conteneurHistorique.getChildren().clear();
        double total = 0;

        if (liste.isEmpty()) {
            Label vide = new Label("Vous n'avez encore rien enregistré.\n"
                    + "Appuyez sur le bouton vert « J'ai fait un entretien » en bas de l'écran pour commencer.");
            vide.getStyleClass().add("historique-vide");
            vide.setWrapText(true);
            vide.setMaxWidth(Double.MAX_VALUE);
            conteneurHistorique.getChildren().add(vide);
        } else {
            for (Entretien e : liste) {
                total += e.getCout();
                conteneurHistorique.getChildren().add(creerLigneHistorique(e));
            }
        }
        totalDepense.setText(liste.isEmpty() ? "" : String.format(Locale.FRANCE, "Total dépensé : %,.2f €", total)
                .replace('\u202F', '\u00A0'));
    }

    private HBox creerLigneHistorique(Entretien e) {
        Label titre = new Label(e.getTypeOperation());
        titre.getStyleClass().add("historique-titre");
        titre.setWrapText(true);

        Label detail = new Label(e.getDateRealisation().format(Formats.DATE_LONGUE) + "   -   "
                + Formats.km(e.getKilometrageRealisation()) + " km");
        detail.getStyleClass().add("historique-detail");

        VBox texte = new VBox(3, titre, detail);
        HBox.setHgrow(texte, Priority.ALWAYS);

        Label prix = new Label(String.format(Locale.FRANCE, "%.2f €", e.getCout()));
        prix.getStyleClass().add("historique-prix");

        Button modifier = Composants.bouton("✏  Modifier", "btn-modif", "btn-petit");
        modifier.setOnAction(ev -> DialogueEntretienController.ouvrir(dashboard, vehicule, e).ifPresent(modifie -> {
            EntretienDB.modifierEntretien(modifie);
            afficher(vehicule);
            dashboard.toast("✔  Modification enregistrée");
        }));

        Button supprimer = Composants.bouton("🗑  Supprimer", "btn-danger", "btn-petit");
        supprimer.setOnAction(ev -> {
            if (DialogueSupport.confirmer(dashboard, "Supprimer cette ligne",
                    "Voulez-vous vraiment supprimer « " + e.getTypeOperation() + " » du "
                            + e.getDateRealisation().format(Formats.DATE_LONGUE) + " ?", "Oui, supprimer")) {
                EntretienDB.supprimerEntretien(e.getId());
                afficher(vehicule);
                dashboard.toast("✔  Ligne supprimée");
            }
        });

        HBox ligne = new HBox(16, texte, prix, modifier, supprimer);
        ligne.setAlignment(Pos.CENTER_LEFT);
        ligne.getStyleClass().add("ligne-historique");
        return ligne;
    }

    // ---------- Actions des boutons du FXML ----------

    /** Retour à la liste des véhicules. */
    @FXML
    public void retourAccueil() {
        dashboard.afficherAccueil();
    }

    /** Ouvre la fenêtre d'aide et de saisie de la clé API. */
    @FXML
    public void ouvrirCleApi() {
        DialogueCleApiController.ouvrir(dashboard).ifPresent(dashboard::toast);
    }

    /** Ouvre la fenêtre de gestion des règles de suivi, puis rafraîchit l'écran. */
    @FXML
    public void ouvrirReglages() {
        DialogueReglagesController.ouvrir(dashboard);
        dashboard.rafraichir();
    }

    /** Met à jour le kilométrage du compteur (refuse une valeur inférieure à l'actuelle). */
    @FXML
    public void mettreAJourKilometrage() {
        if (vehicule == null) return;
        DialogueKilometrageController.ouvrir(dashboard, vehicule).ifPresent(nouveauKm -> {
            vehicule.setKilometrageActuel(nouveauKm);
            VehiculeDB.modifierVehicule(vehicule);
            afficher(vehicule);
            dashboard.toast("✔  Kilométrage mis à jour");
        });
    }

    /** Ouvre la saisie pour modifier le véhicule affiché ; supprime l'ancienne copie de photo si elle a été remplacée ou retirée. */
    @FXML
    public void modifierVehicule() {
        if (vehicule == null) return;
        DialogueVehiculeController.ouvrir(dashboard, vehicule).ifPresent(modifie -> {
            String ancienneCopie = vehicule.getImageLocale();
            VehiculeDB.modifierVehicule(modifie);
            if (ancienneCopie != null && !ancienneCopie.equals(modifie.getImageLocale())) {
                ImageService.supprimer(ancienneCopie);   // photo remplacée ou retirée : on libère la place
            }
            dashboard.definirVehiculeSelectionne(modifie);
            afficher(modifie);
            dashboard.toast("✔  Modifications enregistrées");
        });
    }

    /** Supprime le véhicule affiché après confirmation, avec son historique et sa photo personnelle. */
    @FXML
    public void supprimerVehicule() {
        if (vehicule == null) return;
        boolean oui = DialogueSupport.confirmer(dashboard, "Supprimer ce véhicule",
                "Voulez-vous vraiment supprimer « " + vehicule.getMarque() + " " + vehicule.getModele() + " » ?\n\n"
                        + "Tout son historique d'entretien sera aussi effacé. Cette action est définitive.",
                "Oui, supprimer");
        if (oui) {
            VehiculeDB.supprimerVehicule(vehicule.getId());
            ImageService.supprimer(vehicule.getImageLocale());
            dashboard.afficherAccueil();
            dashboard.toast("✔  Véhicule supprimé");
        }
    }

    /** Enregistre un entretien réalisé pour le véhicule affiché. */
    @FXML
    public void ajouterEntretien() {
        if (vehicule == null) return;
        DialogueEntretienController.ouvrir(dashboard, vehicule, null).ifPresent(nouveau -> {
            EntretienDB.ajouterEntretien(nouveau);
            afficher(vehicule);
            dashboard.toast("✔  Entretien enregistré");
        });
    }

    /** Exporte l'historique du véhicule affiché en CSV (ne fait rien si l'historique est vide). */
    @FXML
    public void exporterHistorique() {
        if (vehicule == null) return;
        List<Entretien> factures = EntretienDB.getEntretiensPourVehicule(vehicule.getId());
        if (factures.isEmpty()) {
            dashboard.toast("Rien à enregistrer : l'historique est vide");
            return;
        }
        ExportService.exporterHistoriqueCSV(vehicule, factures);
        dashboard.toast("✔  Export terminé");
    }
}
