package com.carnetentretien.carnetentretien.controller.vue;

import com.carnetentretien.carnetentretien.controller.DashboardContext;
import com.carnetentretien.carnetentretien.controller.dialogue.DialogueEntretienController;
import com.carnetentretien.carnetentretien.controller.dialogue.DialoguePlanificationController;
import com.carnetentretien.carnetentretien.controller.ui.Composants;
import com.carnetentretien.carnetentretien.controller.ui.DialogueSupport;
import com.carnetentretien.carnetentretien.controller.ui.EntretienUi;
import com.carnetentretien.carnetentretien.controller.ui.Formats;
import com.carnetentretien.carnetentretien.db.EntretienDB;
import com.carnetentretien.carnetentretien.db.EntretienPlanifieDB;
import com.carnetentretien.carnetentretien.db.VehiculeDB;
import com.carnetentretien.carnetentretien.model.Entretien;
import com.carnetentretien.carnetentretien.model.EntretienPlanifie;
import com.carnetentretien.carnetentretien.model.Vehicule;
import com.carnetentretien.carnetentretien.service.CalendrierService;
import com.carnetentretien.carnetentretien.service.EvenementCalendrier;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Contrôleur de l'écran calendrier ({@code vues/calendrier.fxml}) : grille mensuelle, liste
 * des entretiens du mois, échéances en retard et planification d'entretiens.
 */
public class CalendrierController {

    private static final DateTimeFormatter FORMAT_MOIS = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRANCE);
    private static final DateTimeFormatter FORMAT_JOUR = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRANCE);
    private static final String[] JOURS = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};

    /** Nombre maximum d'événements affichés dans une case du calendrier. */
    private static final int MAX_PAR_JOUR = 3;

    @FXML private ComboBox<String> filtreVehicule;
    @FXML private Label titreMois;
    @FXML private GridPane grille;
    @FXML private VBox conteneurListe;
    @FXML private Label resumeMois;
    @FXML private HBox bandeauRetard;
    @FXML private Label messageRetard;

    private DashboardContext dashboard;
    private YearMonth mois = YearMonth.now();
    private Vehicule filtre;
    private List<Vehicule> vehicules = List.of();
    private List<EvenementCalendrier> evenements = List.of();

    /** Évite qu'un rafraîchissement du filtre déclenche à nouveau {@link #changerFiltre()}. */
    private boolean miseAJourFiltre;

    /** Renseigné par {@link com.carnetentretien.carnetentretien.controller.DashboardController} après le chargement du FXML. */
    public void installer(DashboardContext dashboard) {
        this.dashboard = dashboard;
    }

    /**
     * Ouvre le calendrier sur le mois courant.
     *
     * @param filtre véhicule à afficher, ou {@code null} pour tous les véhicules
     */
    public void ouvrir(Vehicule filtre) {
        this.filtre = filtre;
        this.mois = YearMonth.now();
        rafraichir();
    }

    /** Recharge les véhicules et les événements depuis la base, puis redessine l'écran. */
    public void rafraichir() {
        vehicules = VehiculeDB.getTousLesVehicules();
        if (filtre != null) {
            filtre = vehicules.stream()
                    .filter(v -> v.getId() == filtre.getId())
                    .findFirst().orElse(null);
        }
        majFiltre();
        evenements = CalendrierService.charger(filtre);
        dessiner();
    }

    // ---------- Filtre et navigation ----------

    private void majFiltre() {
        miseAJourFiltre = true;
        filtreVehicule.getItems().setAll("Tous les véhicules");
        int selection = 0;
        for (int i = 0; i < vehicules.size(); i++) {
            Vehicule v = vehicules.get(i);
            filtreVehicule.getItems().add(libelle(v));
            if (filtre != null && v.getId() == filtre.getId()) selection = i + 1;
        }
        filtreVehicule.getSelectionModel().select(selection);
        miseAJourFiltre = false;
    }

    @FXML
    private void changerFiltre() {
        if (miseAJourFiltre) return;
        int index = filtreVehicule.getSelectionModel().getSelectedIndex();
        filtre = index <= 0 ? null : vehicules.get(index - 1);
        evenements = CalendrierService.charger(filtre);
        dessiner();
    }

    @FXML
    private void moisPrecedent() {
        mois = mois.minusMonths(1);
        dessiner();
    }

    @FXML
    private void moisSuivant() {
        mois = mois.plusMonths(1);
        dessiner();
    }

    @FXML
    private void moisAujourdhui() {
        mois = YearMonth.now();
        dessiner();
    }

    /** Retour à la liste des véhicules. */
    @FXML
    public void retourAccueil() {
        dashboard.afficherAccueil();
    }

    // ---------- Dessin ----------

    private void dessiner() {
        titreMois.setText(capitaliser(mois.atDay(1).format(FORMAT_MOIS)));
        dessinerGrille();
        dessinerListe();
        dessinerBandeau();
    }

    private void dessinerGrille() {
        grille.getChildren().clear();
        grille.getColumnConstraints().clear();
        for (int i = 0; i < 7; i++) {
            ColumnConstraints colonne = new ColumnConstraints();
            colonne.setPercentWidth(100.0 / 7);
            colonne.setHgrow(Priority.ALWAYS);
            grille.getColumnConstraints().add(colonne);
        }

        for (int i = 0; i < JOURS.length; i++) {
            Label jour = new Label(JOURS[i]);
            jour.getStyleClass().add("jour-semaine");
            jour.setMaxWidth(Double.MAX_VALUE);
            jour.setAlignment(Pos.CENTER);
            grille.add(jour, i, 0);
        }

        Map<LocalDate, List<EvenementCalendrier>> parDate = evenements.stream()
                .collect(Collectors.groupingBy(EvenementCalendrier::getDate));

        LocalDate premier = mois.atDay(1);
        int decalage = premier.getDayOfWeek().getValue() - 1;
        LocalDate debut = premier.minusDays(decalage);
        int semaines = (decalage + mois.lengthOfMonth() + 6) / 7;
        LocalDate aujourdhui = LocalDate.now();

        for (int semaine = 0; semaine < semaines; semaine++) {
            for (int jour = 0; jour < 7; jour++) {
                LocalDate date = debut.plusDays((long) semaine * 7 + jour);
                grille.add(creerCellule(date, parDate.getOrDefault(date, List.of()), aujourdhui),
                        jour, semaine + 1);
            }
        }
    }

    private VBox creerCellule(LocalDate date, List<EvenementCalendrier> duJour, LocalDate aujourdhui) {
        Label numero = new Label(String.valueOf(date.getDayOfMonth()));
        numero.getStyleClass().add("jour-numero");
        HBox entete = new HBox(numero);
        entete.setAlignment(Pos.CENTER_RIGHT);

        VBox liste = new VBox(3);
        int affiches = Math.min(duJour.size(), MAX_PAR_JOUR);
        for (int i = 0; i < affiches; i++) {
            liste.getChildren().add(creerPuce(duJour.get(i)));
        }
        if (duJour.size() > affiches) {
            Label plus = new Label("+" + (duJour.size() - affiches) + " autre" + (duJour.size() - affiches > 1 ? "s" : ""));
            plus.getStyleClass().add("jour-plus");
            liste.getChildren().add(plus);
        }

        VBox cellule = new VBox(4, entete, liste);
        cellule.getStyleClass().add("jour-calendrier");
        if (!YearMonth.from(date).equals(mois)) cellule.getStyleClass().add("jour-hors-mois");
        if (date.equals(aujourdhui)) cellule.getStyleClass().add("jour-aujourdhui");
        cellule.setMaxWidth(Double.MAX_VALUE);
        cellule.setMaxHeight(Double.MAX_VALUE);
        return cellule;
    }

    private Label creerPuce(EvenementCalendrier evenement) {
        Label puce = new Label(icone(evenement) + "  " + evenement.getOperation());
        puce.getStyleClass().addAll("evenement", classeEvenement(evenement));
        if (evenement.isEnRetard()) puce.getStyleClass().add("evenement-retard");
        puce.setMaxWidth(Double.MAX_VALUE);
        puce.setTextOverrun(OverrunStyle.ELLIPSIS);
        puce.setTooltip(new Tooltip(libelleComplet(evenement)));
        puce.setCursor(Cursor.HAND);
        puce.setOnMouseClicked(e -> actionner(evenement));
        return puce;
    }

    private void dessinerListe() {
        conteneurListe.getChildren().clear();

        List<EvenementCalendrier> enRetard = evenements.stream()
                .filter(e -> e.getType() == EvenementCalendrier.Type.ECHEANCE && e.isEnRetard())
                .toList();
        Set<EvenementCalendrier> dejaAffiches = new HashSet<>(enRetard);

        if (!enRetard.isEmpty()) {
            conteneurListe.getChildren().add(titre("⚠  En retard"));
            for (EvenementCalendrier evenement : enRetard) {
                conteneurListe.getChildren().add(creerLigne(evenement));
            }
        }

        List<EvenementCalendrier> duMois = evenements.stream()
                .filter(e -> YearMonth.from(e.getDate()).equals(mois))
                .filter(e -> !dejaAffiches.contains(e))
                .toList();

        conteneurListe.getChildren().add(titre("📅  " + capitaliser(mois.atDay(1).format(FORMAT_MOIS))));

        if (duMois.isEmpty()) {
            Label vide = new Label(enRetard.isEmpty()
                    ? "Aucun entretien ce mois-ci. Utilisez « ◀ » et « ▶ » pour changer de mois, "
                      + "ou planifiez un entretien avec le bouton ci-dessous."
                    : "Aucun autre entretien ce mois-ci.");
            vide.getStyleClass().add("historique-vide");
            vide.setWrapText(true);
            vide.setMaxWidth(Double.MAX_VALUE);
            conteneurListe.getChildren().add(vide);
        } else {
            Map<LocalDate, List<EvenementCalendrier>> parDate = duMois.stream()
                    .collect(Collectors.groupingBy(EvenementCalendrier::getDate, TreeMap::new, Collectors.toList()));
            for (Map.Entry<LocalDate, List<EvenementCalendrier>> entree : parDate.entrySet()) {
                Label jour = new Label(capitaliser(entree.getKey().format(FORMAT_JOUR)));
                jour.getStyleClass().add("jour-liste");
                conteneurListe.getChildren().add(jour);
                for (EvenementCalendrier evenement : entree.getValue()) {
                    conteneurListe.getChildren().add(creerLigne(evenement));
                }
            }
        }

        int nombre = duMois.size();
        resumeMois.setText(nombre == 0 ? "" : nombre + (nombre > 1 ? " événements" : " événement"));
    }

    private Label titre(String texte) {
        Label label = new Label(texte);
        label.getStyleClass().add("titre-section");
        return label;
    }

    private HBox creerLigne(EvenementCalendrier evenement) {
        Label icone = new Label(icone(evenement));
        icone.getStyleClass().addAll("evenement-icone", classeEvenement(evenement));

        Label titre = new Label(evenement.getOperation());
        titre.getStyleClass().add("evenement-titre");
        titre.setWrapText(true);

        Label detail = new Label(detail(evenement));
        detail.getStyleClass().add("evenement-detail");
        detail.setWrapText(true);

        VBox texte = new VBox(3, titre, detail);
        HBox.setHgrow(texte, Priority.ALWAYS);

        HBox ligne = new HBox(14, icone, texte);
        ligne.setAlignment(Pos.CENTER_LEFT);
        ligne.getStyleClass().add("ligne-evenement");
        if (evenement.isEnRetard()) ligne.getStyleClass().add("ligne-evenement-retard");

        switch (evenement.getType()) {
            case PLANIFIE -> {
                Button fait = Composants.bouton("✔  Fait", "btn-succes", "btn-petit");
                fait.setOnAction(e -> marquerFait(evenement));

                Button modifier = Composants.bouton("✏  Modifier", "btn-modif", "btn-petit");
                modifier.setOnAction(e -> modifierPlanification(evenement));

                Button supprimer = Composants.bouton("🗑  Supprimer", "btn-danger", "btn-petit");
                supprimer.setOnAction(e -> supprimerPlanification(evenement));

                ligne.getChildren().addAll(fait, modifier, supprimer);
            }
            case ECHEANCE -> {
                Button planifier = Composants.bouton("📌  Planifier", "btn-secondaire", "btn-petit");
                planifier.setOnAction(e -> planifierEcheance(evenement));

                Button voir = Composants.bouton("👁  Voir", "btn-discret", "btn-petit");
                voir.setOnAction(e -> dashboard.ouvrirVehicule(evenement.getVehicule()));

                ligne.getChildren().addAll(planifier, voir);
            }
            case REALISE -> {
                Button voir = Composants.bouton("👁  Voir", "btn-discret", "btn-petit");
                voir.setOnAction(e -> dashboard.ouvrirVehicule(evenement.getVehicule()));
                ligne.getChildren().add(voir);
            }
        }
        return ligne;
    }

    private void dessinerBandeau() {
        long retard = evenements.stream()
                .filter(e -> e.getType() == EvenementCalendrier.Type.ECHEANCE && e.isEnRetard())
                .count();
        Composants.montrer(bandeauRetard, retard > 0);
        if (retard > 0) {
            messageRetard.setText(retard > 1
                    ? "⚠  " + retard + " entretiens sont en retard. Ils restent affichés ci-dessous."
                    : "⚠  Un entretien est en retard. Il reste affiché ci-dessous.");
        }
    }

    // ---------- Actions ----------

    /** Planifie un entretien pour le véhicule sélectionné (ou n'importe quel véhicule). */
    @FXML
    public void planifier() {
        DialoguePlanificationController.ouvrirCreation(dashboard, filtre, null, null).ifPresent(planifie -> {
            EntretienPlanifieDB.ajouterEntretienPlanifie(planifie);
            rafraichir();
            dashboard.toast("✔  Entretien planifié");
        });
    }

    private void planifierEcheance(EvenementCalendrier evenement) {
        DialoguePlanificationController.ouvrirCreation(dashboard, evenement.getVehicule(),
                evenement.getOperation(), evenement.getDate()).ifPresent(planifie -> {
            EntretienPlanifieDB.ajouterEntretienPlanifie(planifie);
            rafraichir();
            dashboard.toast("✔  Entretien planifié");
        });
    }

    private void modifierPlanification(EvenementCalendrier evenement) {
        DialoguePlanificationController.ouvrirModification(dashboard, evenement.getPlanifie())
                .ifPresent(planifie -> {
                    EntretienPlanifieDB.modifierEntretienPlanifie(planifie);
                    rafraichir();
                    dashboard.toast("✔  Planification modifiée");
                });
    }

    private void supprimerPlanification(EvenementCalendrier evenement) {
        EntretienPlanifie planifie = evenement.getPlanifie();
        if (!DialogueSupport.confirmer(dashboard, "Supprimer cette planification",
                "Voulez-vous vraiment supprimer « " + planifie.getTypeOperation() + " » prévu le "
                        + planifie.getDatePrevue().format(Formats.DATE_LONGUE) + " ?",
                "Oui, supprimer")) {
            return;
        }
        EntretienPlanifieDB.supprimerEntretienPlanifie(planifie.getId());
        rafraichir();
        dashboard.toast("✔  Planification supprimée");
    }

    /** Enregistre l'entretien planifié dans l'historique, puis retire la planification. */
    private void marquerFait(EvenementCalendrier evenement) {
        EntretienPlanifie planifie = evenement.getPlanifie();
        DialogueEntretienController.ouvrir(dashboard, evenement.getVehicule(), null,
                planifie.getTypeOperation(), null).ifPresent(fait -> {
            EntretienDB.ajouterEntretien(fait);
            EntretienPlanifieDB.supprimerEntretienPlanifie(planifie.getId());
            rafraichir();
            dashboard.toast("✔  Entretien enregistré");
        });
    }

    private void actionner(EvenementCalendrier evenement) {
        if (evenement.getType() == EvenementCalendrier.Type.PLANIFIE) {
            modifierPlanification(evenement);
        } else {
            dashboard.ouvrirVehicule(evenement.getVehicule());
        }
    }

    // ---------- Textes ----------

    private String classeEvenement(EvenementCalendrier evenement) {
        return switch (evenement.getType()) {
            case ECHEANCE -> "evenement-echeance-" + EntretienUi.suffixe(EntretienUi.niveauDe(evenement.getPourcentage()));
            case PLANIFIE -> "evenement-planifie";
            case REALISE -> "evenement-realise";
        };
    }

    private String icone(EvenementCalendrier evenement) {
        return switch (evenement.getType()) {
            case ECHEANCE -> "⏰";
            case PLANIFIE -> "📌";
            case REALISE -> "✔";
        };
    }

    private String libelleComplet(EvenementCalendrier evenement) {
        return capitaliser(evenement.getDate().format(FORMAT_JOUR)) + " · " + evenement.getOperation()
                + "\n" + libelle(evenement.getVehicule()) + "\n" + detail(evenement);
    }

    private String detail(EvenementCalendrier evenement) {
        return switch (evenement.getType()) {
            case REALISE -> {
                Entretien entretien = evenement.getEntretien();
                String texte = libelle(evenement.getVehicule()) + " · "
                        + Formats.km(entretien.getKilometrageRealisation()) + " km";
                if (entretien.getCout() > 0) {
                    texte += " · " + String.format(Locale.FRANCE, "%.2f €", entretien.getCout());
                }
                yield texte;
            }
            case PLANIFIE -> {
                EntretienPlanifie planifie = evenement.getPlanifie();
                String texte = libelle(evenement.getVehicule());
                if (planifie.getNote() != null && !planifie.getNote().isBlank()) {
                    texte += " · " + planifie.getNote();
                }
                yield texte;
            }
            case ECHEANCE -> libelle(evenement.getVehicule()) + " · "
                    + (evenement.isEnRetard() ? "En retard" : "Échéance estimée")
                    + " · usure " + Math.round(evenement.getPourcentage()) + " %";
        };
    }

    private String libelle(Vehicule v) {
        String texte = v.getMarque() + " " + v.getModele();
        if (v.getImmatriculation() != null && !v.getImmatriculation().isBlank()) {
            texte += " (" + v.getImmatriculation() + ")";
        }
        return texte;
    }

    private static String capitaliser(String texte) {
        if (texte == null || texte.isEmpty()) return texte;
        return Character.toUpperCase(texte.charAt(0)) + texte.substring(1);
    }
}
