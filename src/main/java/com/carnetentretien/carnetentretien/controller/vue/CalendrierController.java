package com.carnetentretien.carnetentretien.controller.vue;

import com.carnetentretien.carnetentretien.controller.DashboardContext;
import com.carnetentretien.carnetentretien.controller.dialogue.DialogueControleTechniqueController;
import com.carnetentretien.carnetentretien.controller.dialogue.DialogueControleTechniquePlanificationController;
import com.carnetentretien.carnetentretien.controller.dialogue.DialogueEntretienController;
import com.carnetentretien.carnetentretien.controller.dialogue.DialoguePlanificationController;
import com.carnetentretien.carnetentretien.controller.ui.Composants;
import com.carnetentretien.carnetentretien.controller.ui.DialogueSupport;
import com.carnetentretien.carnetentretien.controller.ui.EntretienUi;
import com.carnetentretien.carnetentretien.controller.ui.Formats;
import com.carnetentretien.carnetentretien.db.ControleTechniqueDB;
import com.carnetentretien.carnetentretien.db.ControleTechniquePlanifieDB;
import com.carnetentretien.carnetentretien.db.EntretienDB;
import com.carnetentretien.carnetentretien.db.EntretienPlanifieDB;
import com.carnetentretien.carnetentretien.db.VehiculeDB;
import com.carnetentretien.carnetentretien.model.ControleTechnique;
import com.carnetentretien.carnetentretien.model.ControleTechniquePlanifie;
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
 * des entretiens du mois, échéances en retard, planification d'entretiens et de contrôles techniques.
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
            plus.setCursor(Cursor.HAND);
            plus.setOnMouseClicked(e -> {
                ouvrirJour(date);
                e.consume();
            });
            liste.getChildren().add(plus);
        }

        VBox cellule = new VBox(4, entete, liste);
        cellule.getStyleClass().add("jour-calendrier");
        if (!YearMonth.from(date).equals(mois)) cellule.getStyleClass().add("jour-hors-mois");
        if (date.equals(aujourdhui)) cellule.getStyleClass().add("jour-aujourdhui");
        cellule.setMaxWidth(Double.MAX_VALUE);
        cellule.setMaxHeight(Double.MAX_VALUE);
        if (!duJour.isEmpty()) {
            cellule.setCursor(Cursor.HAND);
            cellule.setOnMouseClicked(e -> ouvrirJour(date));
        }
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
        puce.setOnMouseClicked(e -> {
            actionner(evenement);
            e.consume();
        });
        return puce;
    }

    private void dessinerListe() {
        conteneurListe.getChildren().clear();

        List<EvenementCalendrier> enRetard = evenements.stream()
                .filter(e -> (e.getType() == EvenementCalendrier.Type.ECHEANCE
                        || e.getType() == EvenementCalendrier.Type.CONTROLE) && e.isEnRetard())
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
                    ? "Aucun événement ce mois-ci. Utilisez « ◀ » et « ▶ » pour changer de mois, "
                      + "ou planifiez un entretien ou un contrôle technique avec les boutons ci-dessous."
                    : "Aucun autre événement ce mois-ci.");
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
            case CONTROLE_PLANIFIE -> {
                Button fait = Composants.bouton("✔  Fait", "btn-succes", "btn-petit");
                fait.setOnAction(e -> marquerControleFait(evenement));

                Button modifier = Composants.bouton("✏  Modifier", "btn-modif", "btn-petit");
                modifier.setOnAction(e -> modifierPlanificationControle(evenement));

                Button supprimer = Composants.bouton("🗑  Supprimer", "btn-danger", "btn-petit");
                supprimer.setOnAction(e -> supprimerPlanificationControle(evenement));

                ligne.getChildren().addAll(fait, modifier, supprimer);
            }
            case ECHEANCE -> {
                Button planifier = Composants.bouton("📌  Planifier", "btn-secondaire", "btn-petit");
                planifier.setOnAction(e -> planifierEcheance(evenement));

                Button enregistrer = Composants.bouton("✔  Enregistrer", "btn-succes", "btn-petit");
                enregistrer.setOnAction(e -> enregistrerEntretien(evenement));

                Button voir = Composants.bouton("👁  Voir", "btn-discret", "btn-petit");
                voir.setOnAction(e -> dashboard.ouvrirVehicule(evenement.getVehicule()));

                ligne.getChildren().addAll(planifier, enregistrer, voir);
            }
            case REALISE -> {
                Button voir = Composants.bouton("👁  Voir", "btn-discret", "btn-petit");
                voir.setOnAction(e -> dashboard.ouvrirVehicule(evenement.getVehicule()));
                ligne.getChildren().add(voir);
            }
            case CONTROLE -> {
                Button planifier = Composants.bouton("📌  Planifier", "btn-secondaire", "btn-petit");
                planifier.setOnAction(e -> planifierControle(evenement.getVehicule(), evenement.getDate()));

                Button enregistrer = Composants.bouton("🔍  Enregistrer", "btn-succes", "btn-petit");
                enregistrer.setOnAction(e -> enregistrerControle(evenement));

                Button voir = Composants.bouton("👁  Voir", "btn-discret", "btn-petit");
                voir.setOnAction(e -> dashboard.ouvrirVehicule(evenement.getVehicule()));

                ligne.getChildren().addAll(planifier, enregistrer, voir);
            }
            case CONTROLE_FAIT -> {
                Button voir = Composants.bouton("👁  Voir", "btn-discret", "btn-petit");
                voir.setOnAction(e -> dashboard.ouvrirVehicule(evenement.getVehicule()));
                ligne.getChildren().add(voir);
            }
        }
        return ligne;
    }

    /** Ouvre le détail complet d'une journée : tous les événements, même ceux masqués par « +N autres ». */
    private void ouvrirJour(LocalDate date) {
        List<EvenementCalendrier> duJour = evenements.stream()
                .filter(e -> date.equals(e.getDate()))
                .toList();
        if (duJour.isEmpty()) return;

        Label resume = new Label(duJour.size() == 1 ? "1 événement" : duJour.size() + " événements");
        resume.getStyleClass().add("texte-doux");

        VBox contenu = new VBox(12, resume);
        for (EvenementCalendrier evenement : duJour) {
            contenu.getChildren().add(creerLigneJour(evenement));
        }

        DialogueSupport.afficherDetail(dashboard, capitaliser(date.format(FORMAT_JOUR)), contenu);
    }

    /** Ligne en lecture seule utilisée dans le détail d'une journée. */
    private HBox creerLigneJour(EvenementCalendrier evenement) {
        Label icone = new Label(icone(evenement));
        icone.getStyleClass().addAll("evenement-icone", classeEvenement(evenement));

        Label titre = new Label(evenement.getOperation());
        titre.getStyleClass().add("evenement-titre");
        titre.setWrapText(true);

        Label detail = new Label(detail(evenement));
        detail.getStyleClass().add("evenement-detail");
        detail.setWrapText(true);

        Label type = new Label(libelleType(evenement));
        type.getStyleClass().addAll("badge", "badge-petit", classeBadge(evenement));

        VBox texte = new VBox(3, titre, detail);
        HBox.setHgrow(texte, Priority.ALWAYS);

        HBox ligne = new HBox(14, icone, texte, type);
        ligne.setAlignment(Pos.CENTER_LEFT);
        ligne.getStyleClass().add("ligne-evenement");
        if (evenement.isEnRetard()) ligne.getStyleClass().add("ligne-evenement-retard");
        return ligne;
    }

    private String libelleType(EvenementCalendrier evenement) {
        return switch (evenement.getType()) {
            case ECHEANCE -> "Échéance";
            case CONTROLE -> "Contrôle technique";
            case PLANIFIE -> "Entretien planifié";
            case CONTROLE_PLANIFIE -> "Contrôle planifié";
            case REALISE -> "Entretien réalisé";
            case CONTROLE_FAIT -> "Contrôle réalisé";
        };
    }

    private String classeBadge(EvenementCalendrier evenement) {
        return switch (evenement.getType()) {
            case ECHEANCE -> "badge-" + EntretienUi.suffixe(EntretienUi.niveauDe(evenement.getPourcentage()));
            case CONTROLE -> "badge-" + (evenement.isEnRetard() ? "urgent" : evenement.isProche() ? "prevoir" : "ok");
            case PLANIFIE, CONTROLE_PLANIFIE -> "badge-neutre";
            case REALISE, CONTROLE_FAIT -> "badge-ok";
        };
    }

    private void dessinerBandeau() {
        long retard = evenements.stream()
                .filter(e -> (e.getType() == EvenementCalendrier.Type.ECHEANCE
                        || e.getType() == EvenementCalendrier.Type.CONTROLE) && e.isEnRetard())
                .count();
        Composants.montrer(bandeauRetard, retard > 0);
        if (retard > 0) {
            messageRetard.setText(retard > 1
                    ? "⚠  " + retard + " échéances sont en retard. Elles restent affichées ci-dessous."
                    : "⚠  Une échéance est en retard. Elle reste affichée ci-dessous.");
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

    /** Planifie un contrôle technique pour le véhicule sélectionné (ou n'importe quel véhicule). */
    @FXML
    public void planifierControleTechnique() {
        planifierControle(filtre, null);
    }

    private void planifierControle(Vehicule vehicule, LocalDate dateSuggeree) {
        DialogueControleTechniquePlanificationController.ouvrirCreation(dashboard, vehicule, dateSuggeree)
                .ifPresent(planifie -> {
                    ControleTechniquePlanifieDB.ajouterControleTechniquePlanifie(planifie);
                    rafraichir();
                    dashboard.toast("✔  Contrôle technique planifié");
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

    /** Enregistre un entretien réalisé pour le véhicule de l'échéance. */
    private void enregistrerEntretien(EvenementCalendrier evenement) {
        DialogueEntretienController.ouvrir(dashboard, evenement.getVehicule(), null,
                evenement.getOperation(), null).ifPresent(fait -> {
            EntretienDB.ajouterEntretien(fait);
            rafraichir();
            dashboard.toast("✔  Entretien enregistré");
        });
    }

    /** Enregistre un contrôle technique réalisé pour le véhicule de l'événement. */
    private void enregistrerControle(EvenementCalendrier evenement) {
        DialogueControleTechniqueController.ouvrir(dashboard, evenement.getVehicule(), null)
                .ifPresent(controle -> {
                    ControleTechniqueDB.ajouterControleTechnique(controle);
                    rafraichir();
                    dashboard.toast("✔  Contrôle technique enregistré");
                });
    }

    private void modifierPlanificationControle(EvenementCalendrier evenement) {
        DialogueControleTechniquePlanificationController.ouvrirModification(dashboard, evenement.getControlePlanifie())
                .ifPresent(planifie -> {
                    ControleTechniquePlanifieDB.modifierControleTechniquePlanifie(planifie);
                    rafraichir();
                    dashboard.toast("✔  Planification modifiée");
                });
    }

    private void supprimerPlanificationControle(EvenementCalendrier evenement) {
        ControleTechniquePlanifie planifie = evenement.getControlePlanifie();
        if (!DialogueSupport.confirmer(dashboard, "Supprimer cette planification",
                "Voulez-vous vraiment supprimer le contrôle technique prévu le "
                        + planifie.getDatePrevue().format(Formats.DATE_LONGUE) + " ?",
                "Oui, supprimer")) {
            return;
        }
        ControleTechniquePlanifieDB.supprimerControleTechniquePlanifie(planifie.getId());
        rafraichir();
        dashboard.toast("✔  Planification supprimée");
    }

    /** Enregistre le contrôle planifié dans l'historique, puis retire la planification. */
    private void marquerControleFait(EvenementCalendrier evenement) {
        ControleTechniquePlanifie planifie = evenement.getControlePlanifie();
        LocalDate dateProposee = planifie.getDatePrevue().isAfter(LocalDate.now())
                ? LocalDate.now() : planifie.getDatePrevue();
        DialogueControleTechniqueController.ouvrir(dashboard, evenement.getVehicule(), null, dateProposee)
                .ifPresent(controle -> {
                    ControleTechniqueDB.ajouterControleTechnique(controle);
                    ControleTechniquePlanifieDB.supprimerControleTechniquePlanifie(planifie.getId());
                    rafraichir();
                    dashboard.toast("✔  Contrôle technique enregistré");
                });
    }

    private void actionner(EvenementCalendrier evenement) {
        if (evenement.getType() == EvenementCalendrier.Type.PLANIFIE) {
            modifierPlanification(evenement);
        } else if (evenement.getType() == EvenementCalendrier.Type.CONTROLE_PLANIFIE) {
            modifierPlanificationControle(evenement);
        } else {
            dashboard.ouvrirVehicule(evenement.getVehicule());
        }
    }

    // ---------- Textes ----------

    private String classeEvenement(EvenementCalendrier evenement) {
        return switch (evenement.getType()) {
            case ECHEANCE -> "evenement-echeance-" + EntretienUi.suffixe(EntretienUi.niveauDe(evenement.getPourcentage()));
            case CONTROLE -> "evenement-echeance-"
                    + (evenement.isEnRetard() ? "urgent" : evenement.isProche() ? "prevoir" : "ok");
            case PLANIFIE, CONTROLE_PLANIFIE -> "evenement-planifie";
            case REALISE, CONTROLE_FAIT -> "evenement-realise";
        };
    }

    private String icone(EvenementCalendrier evenement) {
        return switch (evenement.getType()) {
            case ECHEANCE -> "⏰";
            case CONTROLE -> "🔍";
            case PLANIFIE, CONTROLE_PLANIFIE -> "📌";
            case REALISE, CONTROLE_FAIT -> "✔";
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
            case CONTROLE_PLANIFIE -> {
                ControleTechniquePlanifie planifie = evenement.getControlePlanifie();
                String texte = libelle(evenement.getVehicule());
                if (planifie.getNote() != null && !planifie.getNote().isBlank()) {
                    texte += " · " + planifie.getNote();
                }
                yield texte;
            }
            case ECHEANCE -> libelle(evenement.getVehicule()) + " · "
                    + (evenement.isEnRetard() ? "En retard" : "Échéance estimée")
                    + " · usure " + Math.round(evenement.getPourcentage()) + " %";
            case CONTROLE -> {
                ControleTechnique controle = evenement.getControle();
                String etat = evenement.isEnRetard() ? "En retard"
                        : evenement.isProche() ? "À prévoir" : "À jour";
                yield libelle(evenement.getVehicule()) + " · " + etat
                        + " · dernier contrôle le " + controle.getDateRealisation().format(Formats.DATE_LONGUE)
                        + " (" + controle.getResultat().getLibelle() + ")";
            }
            case CONTROLE_FAIT -> {
                ControleTechnique controle = evenement.getControle();
                String texte = libelle(evenement.getVehicule()) + " · " + controle.getResultat().getLibelle()
                        + " · prochain contrôle le " + controle.getDateProchaine().format(Formats.DATE_LONGUE);
                if (controle.getNote() != null && !controle.getNote().isBlank()) {
                    texte += " · " + controle.getNote();
                }
                yield texte;
            }
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
