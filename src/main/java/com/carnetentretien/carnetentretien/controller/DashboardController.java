package com.carnetentretien.carnetentretien.controller;

import com.carnetentretien.carnetentretien.db.EntretienDB;
import com.carnetentretien.carnetentretien.db.PreconisationDB;
import com.carnetentretien.carnetentretien.db.VehiculeDB;
import com.carnetentretien.carnetentretien.model.Entretien;
import com.carnetentretien.carnetentretien.model.Preconisation;
import com.carnetentretien.carnetentretien.model.Vehicule;
import com.carnetentretien.carnetentretien.service.CalculateurEntretien;
import com.carnetentretien.carnetentretien.service.ExportService;
import com.carnetentretien.carnetentretien.service.RecherchePlaqueService;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public class DashboardController {

    private static final String FEUILLE_STYLE = "/com/carnetentretien/carnetentretien/style.css";
    private static final DateTimeFormatter FORMAT_DATE =
            DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Locale.FRANCE);

    // Niveaux d'état
    private static final int OK = 0;
    private static final int PREVOIR = 1;
    private static final int URGENT = 2;
    private static final int INCONNU = 3;
    private static final int AUCUNE_REGLE = -1;

    @FXML private StackPane racine;
    @FXML private VBox vueAccueil;
    @FXML private VBox vueVehicule;
    @FXML private VBox blocBienvenue;
    @FXML private FlowPane conteneurVehicules;
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
    @FXML private Label toast;

    private Vehicule vehiculeSelectionne = null;
    private SequentialTransition animationToast;

    @FXML
    public void initialize() {
        installerReglesParDefaut();
        afficherAccueil();
    }

    /** Au tout premier lancement, on propose des règles courantes pour que l'écran ne soit jamais vide. */
    private void installerReglesParDefaut() {
        if (!PreconisationDB.getToutesLesPreconisations().isEmpty()) return;

        Object[][] defauts = {
                {"Vidange", 15000, 12},
                {"Filtre à air", 30000, 24},
                {"Filtre d'habitacle", 15000, 12},
                {"Plaquettes de frein", 40000, 48},
                {"Liquide de frein", 60000, 24},
                {"Pneus", 40000, 48},
                {"Courroie de distribution", 120000, 72}
        };
        for (Object[] d : defauts) {
            PreconisationDB.ajouterPreconisation(
                    new Preconisation(0, (String) d[0], (Integer) d[1], (Integer) d[2]));
        }
    }

    private void afficherAccueil() {
        vehiculeSelectionne = null;
        montrer(vueVehicule, false);
        montrer(vueAccueil, true);
        chargerVehicules();
    }

    @FXML
    public void retourAccueil() {
        afficherAccueil();
    }

    private void ouvrirVehicule(Vehicule v) {
        vehiculeSelectionne = v;
        montrer(vueAccueil, false);
        montrer(vueVehicule, true);
        afficherVehicule();
    }

    private void chargerVehicules() {
        List<Vehicule> vehicules = VehiculeDB.getTousLesVehicules();
        conteneurVehicules.getChildren().clear();
        montrer(blocBienvenue, vehicules.isEmpty());

        for (Vehicule v : vehicules) {
            conteneurVehicules.getChildren().add(creerCarteVehicule(v));
        }
    }

    private Button creerCarteVehicule(Vehicule v) {
        StackPane photo = creerVignette(v.getImageUrl(), 260, 130);

        Label nom = new Label(v.getMarque() + " " + v.getModele());
        nom.getStyleClass().add("carte-titre");
        nom.setWrapText(true);
        nom.setAlignment(Pos.CENTER);
        nom.setMaxWidth(260);

        Label compteur = new Label(km(v.getKilometrageActuel()) + " km");
        compteur.getStyleClass().add("carte-km");

        Label etat = creerBadge(niveauGlobal(analyser(v)));

        VBox contenu = new VBox(8, photo, nom);
        if (v.getImmatriculation() != null && !v.getImmatriculation().isBlank()) {
            contenu.getChildren().add(creerPlaque(v.getImmatriculation()));
        }
        contenu.getChildren().addAll(compteur, etat);
        contenu.setAlignment(Pos.CENTER);

        Button carte = new Button();
        carte.setGraphic(contenu);
        carte.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        carte.getStyleClass().add("carte-vehicule");
        carte.setOnAction(e -> ouvrirVehicule(v));
        return carte;
    }

    @FXML
    public void ajouterVehicule() {
        Vehicule nouveau = saisirVehicule(null);
        if (nouveau != null) {
            VehiculeDB.ajouterVehicule(nouveau);
            chargerVehicules();
            toast("✔  Véhicule enregistré");
        }
    }

    private void afficherVehicule() {
        Vehicule v = vehiculeSelectionne;
        if (v == null) return;

        titreVehicule.setText(v.getMarque() + " " + v.getModele());
        chargerImage(photoVehicule, iconeVehicule, v.getImageUrl());
        zonePlaque.getChildren().clear();
        if (v.getImmatriculation() != null && !v.getImmatriculation().isBlank()) {
            zonePlaque.getChildren().add(creerPlaque(v.getImmatriculation()));
        }
        montrer(zonePlaque, !zonePlaque.getChildren().isEmpty());
        sousTitreVehicule.setText("Compteur : " + km(v.getKilometrageActuel()) + " km   -   environ " + km(v.getKilometrageAnnuelEstime()) + " km par an");

        List<CalculateurEntretien.Bilan> bilans = analyser(v);
        afficherBandeau(bilans);

        conteneurJauges.getChildren().clear();
        if (bilans.isEmpty()) {
            Label vide = new Label("Aucun entretien n'est surveillé pour le moment.\n" + "Appuyez sur « ⚙ Réglages » en haut de l'écran pour en ajouter.");
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
        int niveau = niveauGlobal(bilans);
        String message;
        switch (niveau) {
            case URGENT -> {
                long n = bilans.stream().filter(b -> niveauDe(b.pourcentage) == URGENT).count();
                message = "⚠  " + (n > 1 ? n + " entretiens sont à faire maintenant" : "Un entretien est à faire maintenant");
            }
            case PREVOIR -> message = "⏰  Pensez à prévoir un entretien bientôt";
            case OK -> message = "✔  Tout va bien : rien à faire pour le moment";
            case INCONNU -> message = "ℹ  Ajoutez vos entretiens déjà faits pour recevoir des conseils";
            default -> message = "ℹ  Aucun entretien à surveiller : ouvrez les « Réglages »";
        }
        messageBandeau.setText(message);
        bandeauEtat.getStyleClass().removeAll("bandeau-ok", "bandeau-prevoir", "bandeau-urgent", "bandeau-neutre");
        bandeauEtat.getStyleClass().add("bandeau-" + suffixe(niveau));
    }

    private VBox creerCarteJauge(CalculateurEntretien.Bilan b) {
        int niveau = niveauDe(b.pourcentage);

        Label nom = new Label(b.piece);
        nom.getStyleClass().add("jauge-titre");
        nom.setWrapText(true);

        Region espace = new Region();
        HBox.setHgrow(espace, Priority.ALWAYS);

        HBox haut = new HBox(12, nom, espace, creerBadge(niveau));
        haut.setAlignment(Pos.CENTER_LEFT);

        ProgressBar barre = new ProgressBar(Math.max(0, Math.min(b.pourcentage / 100.0, 1.0)));
        barre.setMaxWidth(Double.MAX_VALUE);
        barre.getStyleClass().add("barre-" + suffixe(niveau));

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
        carte.getStyleClass().addAll("carte-jauge", "jauge-" + suffixe(niveau));
        return carte;
    }

    private void afficherHistorique(Vehicule v) {
        List<Entretien> liste = new ArrayList<>(EntretienDB.getEntretiensPourVehicule(v.getId()));
        liste.sort(Comparator.comparing(Entretien::getDateRealisation).reversed());

        conteneurHistorique.getChildren().clear();
        double total = 0;

        if (liste.isEmpty()) {
            Label vide = new Label("Vous n'avez encore rien enregistré.\n" + "Appuyez sur le bouton vert « J'ai fait un entretien » en bas de l'écran pour commencer.");
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
        totalDepense.setText(liste.isEmpty() ? "" : String.format(Locale.FRANCE, "Total dépensé : %,.2f €", total).replace('\u202F', '\u00A0'));
    }

    private HBox creerLigneHistorique(Entretien e) {
        Label titre = new Label(e.getTypeOperation());
        titre.getStyleClass().add("historique-titre");
        titre.setWrapText(true);

        Label detail = new Label(e.getDateRealisation().format(FORMAT_DATE) + "   -   " + km(e.getKilometrageRealisation()) + " km");
        detail.getStyleClass().add("historique-detail");

        VBox texte = new VBox(3, titre, detail);
        HBox.setHgrow(texte, Priority.ALWAYS);

        Label prix = new Label(String.format(Locale.FRANCE, "%.2f €", e.getCout()));
        prix.getStyleClass().add("historique-prix");

        Button modifier = bouton("✏  Modifier", "btn-modif", "btn-petit");
        modifier.setOnAction(ev -> {
            Entretien modifie = saisirEntretien(e);
            if (modifie != null) {
                EntretienDB.modifierEntretien(modifie);
                afficherVehicule();
                toast("✔  Modification enregistrée");
            }
        });

        Button supprimer = bouton("🗑  Supprimer", "btn-danger", "btn-petit");
        supprimer.setOnAction(ev -> {
            if (confirmer("Supprimer cette ligne", "Voulez-vous vraiment supprimer « " + e.getTypeOperation() + " » du " + e.getDateRealisation().format(FORMAT_DATE) + " ?", "Oui, supprimer")) {
                EntretienDB.supprimerEntretien(e.getId());
                afficherVehicule();
                toast("✔  Ligne supprimée");
            }
        });

        HBox ligne = new HBox(16, texte, prix, modifier, supprimer);
        ligne.setAlignment(Pos.CENTER_LEFT);
        ligne.getStyleClass().add("ligne-historique");
        return ligne;
    }

    @FXML
    public void modifierVehicule() {
        if (vehiculeSelectionne == null) return;
        Vehicule modifie = saisirVehicule(vehiculeSelectionne);
        if (modifie != null) {
            VehiculeDB.modifierVehicule(modifie);
            vehiculeSelectionne = modifie;
            afficherVehicule();
            toast("✔  Modifications enregistrées");
        }
    }

    @FXML
    public void supprimerVehicule() {
        if (vehiculeSelectionne == null) return;
        boolean oui = confirmer("Supprimer ce véhicule", "Voulez-vous vraiment supprimer « " + vehiculeSelectionne.getMarque() + " " + vehiculeSelectionne.getModele() + " » ?\n\n" + "Tout son historique d'entretien sera aussi effacé. Cette action est définitive.", "Oui, supprimer");
        if (oui) {
            VehiculeDB.supprimerVehicule(vehiculeSelectionne.getId());
            afficherAccueil();
            toast("✔  Véhicule supprimé");
        }
    }

    @FXML
    public void mettreAJourKilometrage() {
        if (vehiculeSelectionne == null) return;
        Vehicule v = vehiculeSelectionne;

        TextField champ = champ("Ex : " + (v.getKilometrageActuel() + 500));
        champ.setText(String.valueOf(v.getKilometrageActuel()));

        boolean ok = demander("Mettre à jour le kilométrage",
                "Regardez le compteur de votre voiture et écrivez le nombre affiché.",
                List.of(groupe("Que indique votre compteur aujourd'hui ?", champ, "Écrivez uniquement des chiffres, sans le « km ».")), "✔  Enregistrer", () -> {
                    if (!estEntier(champ.getText())) return "Le kilométrage doit être un nombre, par exemple 45000.";
                    if (parseEntier(champ.getText()) < v.getKilometrageActuel())
                        return "Ce nombre est plus petit que l'ancien (" + km(v.getKilometrageActuel()) + " km). Vérifiez votre compteur, ou utilisez « Modifier » pour corriger une erreur.";
                    return null;
                });
        if (ok) {
            v.setKilometrageActuel(parseEntier(champ.getText()));
            VehiculeDB.modifierVehicule(v);
            afficherVehicule();
            toast("✔  Kilométrage mis à jour");
        }
    }

    @FXML
    public void ajouterEntretien() {
        if (vehiculeSelectionne == null) return;
        Entretien nouveau = saisirEntretien(null);
        if (nouveau != null) {
            EntretienDB.ajouterEntretien(nouveau);
            afficherVehicule();
            toast("✔  Entretien enregistré");
        }
    }

    @FXML
    public void exporterHistorique() {
        if (vehiculeSelectionne == null) return;
        List<Entretien> factures = EntretienDB.getEntretiensPourVehicule(vehiculeSelectionne.getId());
        if (factures.isEmpty()) {
            toast("Rien à enregistrer : l'historique est vide");
            return;
        }
        ExportService.exporterHistoriqueCSV(vehiculeSelectionne, factures);
        toast("✔  Export terminé");
    }

    @FXML
    public void ouvrirReglages() {
        Dialog<ButtonType> dialog = new Dialog<>();
        preparer(dialog, "Réglages des entretiens");

        ButtonType fermer = new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(fermer);
        dialog.getDialogPane().lookupButton(fermer).getStyleClass().addAll("btn", "btn-primaire");

        Label intro = new Label("Voici la liste des entretiens que l'application surveille pour vous, " + "avec leur fréquence. Vous pouvez la changer si votre carnet constructeur indique autre chose.");
        intro.setWrapText(true);
        intro.getStyleClass().add("texte-doux");

        VBox liste = new VBox(10);
        liste.setPadding(new Insets(4));
        remplirReglages(liste);

        ScrollPane defilement = new ScrollPane(liste);
        defilement.setFitToWidth(true);
        defilement.setPrefHeight(340);
        defilement.getStyleClass().add("zone-defilement");

        Button ajouter = bouton("➕  Ajouter un entretien à surveiller", "btn-succes");
        ajouter.setOnAction(e -> {
            Preconisation p = saisirRegle(null);
            if (p != null) {
                PreconisationDB.ajouterPreconisation(p);
                remplirReglages(liste);
            }
        });

        VBox contenu = new VBox(16, intro, defilement, ajouter);
        contenu.setPrefWidth(660);
        contenu.setPadding(new Insets(10, 10, 0, 10));
        dialog.getDialogPane().setContent(contenu);

        dialog.showAndWait();

        if (vehiculeSelectionne != null) afficherVehicule(); else chargerVehicules();
    }

    private void remplirReglages(VBox liste) {
        liste.getChildren().clear();
        List<Preconisation> regles = PreconisationDB.getToutesLesPreconisations();

        if (regles.isEmpty()) {
            Label vide = new Label("Aucun entretien surveillé. Appuyez sur le bouton vert ci-dessous pour en ajouter un.");
            vide.getStyleClass().add("historique-vide");
            vide.setWrapText(true);
            liste.getChildren().add(vide);
            return;
        }

        for (Preconisation p : regles) {
            Label nom = new Label(p.getTypeOperation());
            nom.getStyleClass().add("historique-titre");
            nom.setWrapText(true);
            Label freq = new Label("Tous les " + km(p.getIntervalleKm()) + " km ou tous les " + p.getIntervalleMois() + " mois");
            freq.getStyleClass().add("historique-detail");
            VBox texte = new VBox(3, nom, freq);
            HBox.setHgrow(texte, Priority.ALWAYS);

            Button modifier = bouton("✏  Modifier", "btn-modif", "btn-petit");
            modifier.setOnAction(e -> {
                Preconisation modifiee = saisirRegle(p);
                if (modifiee != null) {
                    PreconisationDB.modifierPreconisation(modifiee);
                    remplirReglages(liste);
                }
            });

            Button supprimer = bouton("🗑  Supprimer", "btn-danger", "btn-petit");
            supprimer.setOnAction(e -> {
                if (confirmer("Ne plus surveiller",
                        "Voulez-vous arrêter de surveiller « " + p.getTypeOperation() + " » ?", "Oui, arrêter")) {
                    PreconisationDB.supprimerPreconisation(p.getId());
                    remplirReglages(liste);
                }
            });

            HBox ligne = new HBox(14, texte, modifier, supprimer);
            ligne.setAlignment(Pos.CENTER_LEFT);
            ligne.getStyleClass().add("ligne-historique");
            liste.getChildren().add(ligne);
        }
    }

    /** Retourne le véhicule saisi, ou null si l'utilisateur a annulé. */
    private Vehicule saisirVehicule(Vehicule existant) {
        TextField plaque = champ("AB-123-CD");
        plaque.getStyleClass().add("champ-plaque");
        plaque.setTextFormatter(new TextFormatter<String>(c -> {
            c.setText(c.getText().toUpperCase());
            return c;
        }));
        TextField marque = champ("Ex : Renault");
        TextField modele = champ("Ex : Clio");
        TextField kmCompteur = champ("Ex : 45000");
        TextField kmAnnuel = champ("Ex : 12000");

        // Photo trouvée via la plaque + plaque à laquelle cette photo correspond
        String[] imageUrl = { existant != null ? existant.getImageUrl() : null };
        String[] plaqueDeLaPhoto = { existant != null ? RecherchePlaqueService.normaliser(existant.getImmatriculation()) : "" };

        // ---------- Aperçu : photo + nom du véhicule trouvé ----------
        ImageView photo = new ImageView();
        photo.setFitWidth(210);
        photo.setFitHeight(125);
        photo.setPreserveRatio(true);
        photo.setVisible(false);
        Label repli = new Label("🚗");
        repli.getStyleClass().add("carte-icone");
        StackPane cadre = new StackPane(repli, photo);
        cadre.getStyleClass().add("photo-cadre");
        cadre.setMinSize(230, 135);
        cadre.setPrefSize(230, 135);
        cadre.setMaxSize(230, 135);

        Label apercuTitre = new Label();
        apercuTitre.getStyleClass().add("apercu-titre");
        apercuTitre.setWrapText(true);
        Label apercuDetail = new Label();
        apercuDetail.getStyleClass().add("apercu-detail");
        apercuDetail.setWrapText(true);
        Label apercuStatut = new Label();
        apercuStatut.setWrapText(true);

        VBox textes = new VBox(6, apercuTitre, apercuDetail, apercuStatut);
        textes.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textes, Priority.ALWAYS);
        HBox apercu = new HBox(18, cadre, textes);
        apercu.setAlignment(Pos.CENTER_LEFT);
        apercu.getStyleClass().add("apercu-vehicule");

        if (existant != null) {
            apercuTitre.setText(existant.getMarque() + " " + existant.getModele());
            definirStatut(apercuStatut, "Pour changer la photo, modifiez la plaque puis appuyez sur « Rechercher ».", "apercu-attente");
            chargerImage(photo, repli, existant.getImageUrl());
        } else {
            apercuTitre.setText("Votre véhicule");
            definirStatut(apercuStatut, "Écrivez la plaque puis appuyez sur « Rechercher » : la photo et le nom de votre voiture apparaîtront ici.", "apercu-attente");
        }

        // ---------- Ligne « plaque + bouton Rechercher » ----------
        Button btnRecherche = bouton("🔍  Rechercher", "btn-primaire", "btn-moyen");
        HBox lignePlaque = new HBox(12, plaque, btnRecherche);
        lignePlaque.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(plaque, Priority.ALWAYS);

        Runnable lancerRecherche = () -> {
            String saisie = plaque.getText();
            if (saisie == null || saisie.isBlank()) {
                definirStatut(apercuStatut, "⚠  Écrivez d'abord la plaque de votre véhicule (exemple : AB-123-CD).", "apercu-erreur");
                plaque.requestFocus();
                return;
            }
            if (!RecherchePlaqueService.formatValide(saisie)) {
                definirStatut(apercuStatut, "⚠  Cette plaque ne ressemble pas à une plaque française. Exemple : AB-123-CD.", "apercu-erreur");
                return;
            }

            btnRecherche.setDisable(true);
            btnRecherche.setText("⏳  Recherche…");
            definirStatut(apercuStatut, "Recherche en cours, quelques secondes…", "apercu-attente");

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
                    imageUrl[0] = r.imageUrl();
                    plaqueDeLaPhoto[0] = RecherchePlaqueService.normaliser(saisie);

                    apercuTitre.setText(r.marque() + " " + r.modele());
                    List<String> details = new ArrayList<>();
                    if (r.annee() != null) details.add("mise en circulation " + r.annee());
                    if (r.energie() != null && !r.energie().isBlank()) details.add(r.energie());
                    if (r.version() != null && !r.version().isBlank()) details.add(r.version());
                    apercuDetail.setText(String.join("  ·  ", details));
                    chargerImage(photo, repli, r.imageUrl());

                    definirStatut(apercuStatut, "✔  Véhicule trouvé ! Vérifiez, puis complétez le kilométrage ci-dessous." + (r.imageUrl() == null ? "  (Pas de photo disponible pour ce modèle.)" : ""), "apercu-ok");
                } else {
                    definirStatut(apercuStatut, "⚠  " + r.messageErreur() + "  Vous pouvez aussi écrire la marque et le modèle vous-même ci-dessous.", "apercu-erreur");
                }
            });
            tache.setOnFailed(ev -> {
                btnRecherche.setDisable(false);
                btnRecherche.setText("🔍  Rechercher");
                definirStatut(apercuStatut, "⚠  La recherche a échoué. Vérifiez votre connexion Internet, " + "ou écrivez la marque et le modèle vous-même ci-dessous.", "apercu-erreur");
            });
            Thread fil = new Thread(tache, "recherche-plaque");
            fil.setDaemon(true);
            fil.start();
        };
        btnRecherche.setOnAction(e -> lancerRecherche.run());
        plaque.setOnAction(e -> lancerRecherche.run());   // touche Entrée

        if (existant != null) {
            plaque.setText(existant.getImmatriculation() != null ? existant.getImmatriculation() : "");
            marque.setText(existant.getMarque());
            modele.setText(existant.getModele());
            kmCompteur.setText(String.valueOf(existant.getKilometrageActuel()));
            kmAnnuel.setText(String.valueOf(existant.getKilometrageAnnuelEstime()));
        } else {
            kmAnnuel.setText("12000");
        }

        boolean ok = demander(
                existant == null ? "Ajouter un véhicule" : "Modifier le véhicule",
                "Commencez par la plaque : nous retrouvons la marque, le modèle et une photo. " + "Pas de plaque sous la main ? Remplissez simplement les cases à la main.",
                List.of(
                        groupe("Plaque d'immatriculation", lignePlaque, "Facultatif, mais c'est elle qui permet d'afficher la photo de votre voiture."),
                        apercu,
                        deuxColonnes(groupe("Marque", marque, null), groupe("Modèle", modele, null)),
                        deuxColonnes(groupe("Kilomètres au compteur", kmCompteur, "Chiffres uniquement. Ex : 45000"), groupe("Kilomètres par an (environ)", kmAnnuel, "Au doute, gardez 12000 : c'est la moyenne en France."))), "✔  Enregistrer",
                () -> {
                    if (!plaque.getText().isBlank() && !RecherchePlaqueService.formatValide(plaque.getText()))
                        return "La plaque n'est pas valide (exemple : AB-123-CD). Corrigez-la, ou videz la case si vous ne l'avez pas.";
                    if (marque.getText().isBlank()) return "Veuillez écrire la marque de la voiture (exemple : Renault).";
                    if (modele.getText().isBlank()) return "Veuillez écrire le modèle de la voiture (exemple : Clio).";
                    if (!estEntier(kmCompteur.getText())) return "Le kilométrage du compteur doit être un nombre, par exemple 45000.";
                    if (!estEntier(kmAnnuel.getText()) || parseEntier(kmAnnuel.getText()) == 0)
                        return "Les kilomètres par an doivent être un nombre supérieur à 0, par exemple 12000.";
                    return null;
                });

        if (!ok) return null;

        String plaqueFinale = plaque.getText().isBlank() ? null : RecherchePlaqueService.formater(plaque.getText());
        // Si la plaque a changé depuis la recherche, la photo ne correspond plus : on ne la garde pas.
        String photoFinale = RecherchePlaqueService.normaliser(plaque.getText()).equals(plaqueDeLaPhoto[0]) ? imageUrl[0] : null;

        return new Vehicule(existant != null ? existant.getId() : 0,
                plaqueFinale, marque.getText().trim(), modele.getText().trim(),
                parseEntier(kmCompteur.getText()), parseEntier(kmAnnuel.getText()), photoFinale);
    }

    // ---------- Photo, plaque, mise en page ----------

    /** Cadre avec la photo du véhicule ; affiche 🚗 tant que la photo n'est pas chargée (ou s'il n'y en a pas). */
    private StackPane creerVignette(String url, double largeur, double hauteur) {
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

    /** Charge l'image en arrière-plan ; en cas d'échec (hors ligne, lien mort) on garde l'icône 🚗. */
    private void chargerImage(ImageView vue, Node repli, String url) {
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
    private HBox creerPlaque(String texte) {
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

    private void definirStatut(Label label, String texte, String classe) {
        label.getStyleClass().removeAll("apercu-ok", "apercu-erreur", "apercu-attente");
        label.getStyleClass().add(classe);
        label.setText(texte);
        if (label.getScene() != null && label.getScene().getWindow() != null) {
            label.getScene().getWindow().sizeToScene();   // la fenêtre s'adapte à la longueur du message
        }
    }

    private HBox deuxColonnes(Node gauche, Node droite) {
        HBox ligne = new HBox(18, gauche, droite);
        for (Node n : new Node[]{gauche, droite}) {
            HBox.setHgrow(n, Priority.ALWAYS);
            ((Region) n).setPrefWidth(10);
            ((Region) n).setMinWidth(0);
        }
        return ligne;
    }

    /** Retourne l'entretien saisi, ou null si l'utilisateur a annulé. */
    private Entretien saisirEntretien(Entretien existant) {
        ComboBox<String> type = new ComboBox<>();
        type.setEditable(true);
        type.setMaxWidth(Double.MAX_VALUE);
        type.setPromptText("Choisissez dans la liste");
        for (Preconisation p : PreconisationDB.getToutesLesPreconisations()) {
            type.getItems().add(p.getTypeOperation());
        }
        if (existant != null) type.setValue(existant.getTypeOperation());

        DatePicker date = new DatePicker(existant != null ? existant.getDateRealisation() : LocalDate.now());
        date.setMaxWidth(Double.MAX_VALUE);

        TextField kmField = champ("Ex : 45000");
        kmField.setText(String.valueOf(existant != null ? existant.getKilometrageRealisation()
                : vehiculeSelectionne.getKilometrageActuel()));

        TextField coutField = champ("Ex : 89,90");
        if (existant != null) coutField.setText(String.format(Locale.FRANCE, "%.2f", existant.getCout()));

        boolean ok = demander(
                existant == null ? "J'ai fait un entretien" : "Modifier l'entretien",
                "Répondez aux 4 questions, puis appuyez sur « Enregistrer ».",
                List.of(
                        groupe("Qu'est-ce qui a été fait ?", type, "Cliquez sur la flèche pour voir la liste, ou écrivez vous-même."),
                        groupe("À quelle date ?", date, "Aujourd'hui est déjà choisi. Cliquez sur le calendrier pour changer."),
                        groupe("Combien de kilomètres au compteur ce jour-là ?", kmField, null),
                        groupe("Combien cela a-t-il coûté ? (en euros)", coutField, "Si vous ne savez plus, laissez vide.")),
                "✔  Enregistrer",
                () -> {
                    String nom = type.getEditor().getText();
                    if (nom == null || nom.isBlank()) return "Veuillez indiquer ce qui a été fait (exemple : Vidange).";
                    if (date.getValue() == null) return "Veuillez choisir la date de l'entretien.";
                    if (date.getValue().isAfter(LocalDate.now())) return "La date ne peut pas être dans le futur.";
                    if (!estEntier(kmField.getText())) return "Le kilométrage doit être un nombre, par exemple 45000.";
                    if (!estMontant(coutField.getText())) return "Le prix doit être un nombre, par exemple 89,90.";
                    return null;
                });

        if (!ok) return null;
        return new Entretien(existant != null ? existant.getId() : 0, vehiculeSelectionne.getId(),
                type.getEditor().getText().trim(), date.getValue(),
                parseEntier(kmField.getText()), parseMontant(coutField.getText()));
    }

    /** Retourne la règle saisie, ou null si l'utilisateur a annulé. */
    private Preconisation saisirRegle(Preconisation existante) {
        TextField nom = champ("Ex : Vidange");
        TextField kmField = champ("Ex : 15000");
        TextField mois = champ("Ex : 12");
        if (existante != null) {
            nom.setText(existante.getTypeOperation());
            kmField.setText(String.valueOf(existante.getIntervalleKm()));
            mois.setText(String.valueOf(existante.getIntervalleMois()));
        }

        boolean ok = demander(
                existante == null ? "Nouvel entretien à surveiller" : "Modifier la règle",
                "Ces informations se trouvent dans le carnet d'entretien fourni avec la voiture.",
                List.of(
                        groupe("Nom de l'entretien", nom, null),
                        groupe("À renouveler tous les combien de kilomètres ?", kmField, "Exemple : 15000"),
                        groupe("… ou tous les combien de mois ?", mois, "Exemple : 12 (le premier des deux arrivé déclenche l'alerte)")),
                "✔  Enregistrer",
                () -> {
                    if (nom.getText().isBlank()) return "Veuillez donner un nom à l'entretien (exemple : Vidange).";
                    if (!estEntier(kmField.getText()) || parseEntier(kmField.getText()) == 0)
                        return "Les kilomètres doivent être un nombre supérieur à 0, par exemple 15000.";
                    if (!estEntier(mois.getText()) || parseEntier(mois.getText()) == 0)
                        return "Les mois doivent être un nombre supérieur à 0, par exemple 12.";
                    return null;
                });

        if (!ok) return null;
        return new Preconisation(existante != null ? existante.getId() : 0, nom.getText().trim(),
                parseEntier(kmField.getText()), parseEntier(mois.getText()));
    }

    /**
     * Affiche une fenêtre de saisie. Elle reste ouverte tant que la validation renvoie un message d'erreur,
     * lequel est affiché en rouge dans la fenêtre. Retourne true si l'utilisateur a validé.
     */
    private boolean demander(String titre, String consigne, List<Node> champs, String texteOk, Supplier<String> validation) {
        Dialog<ButtonType> dialog = new Dialog<>();
        preparer(dialog, titre);

        ButtonType ok = new ButtonType(texteOk, ButtonBar.ButtonData.OK_DONE);
        ButtonType annuler = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(ok, annuler);

        Label intro = new Label(consigne);
        intro.setWrapText(true);
        intro.getStyleClass().add("texte-doux");

        Label erreur = new Label();
        erreur.setWrapText(true);
        erreur.setMaxWidth(Double.MAX_VALUE);
        erreur.getStyleClass().add("message-erreur");
        montrer(erreur, false);

        VBox contenu = new VBox(18);
        contenu.setPadding(new Insets(10, 10, 0, 10));
        contenu.setPrefWidth(640);
        contenu.getChildren().add(intro);
        contenu.getChildren().addAll(champs);
        contenu.getChildren().add(erreur);
        dialog.getDialogPane().setContent(contenu);

        Button boutonOk = (Button) dialog.getDialogPane().lookupButton(ok);
        boutonOk.getStyleClass().addAll("btn", "btn-succes");
        dialog.getDialogPane().lookupButton(annuler).getStyleClass().addAll("btn", "btn-secondaire");

        boutonOk.addEventFilter(ActionEvent.ACTION, ev -> {
            String message = validation.get();
            if (message != null) {
                erreur.setText("⚠  " + message);
                montrer(erreur, true);
                ev.consume();               // la fenêtre reste ouverte
                dialog.getDialogPane().getScene().getWindow().sizeToScene();
            }
        });

        return dialog.showAndWait().filter(r -> r == ok).isPresent();
    }

    /** Fenêtre « Êtes-vous sûr ? ». Le bouton par défaut est « Non » pour éviter les erreurs. */
    private boolean confirmer(String titre, String question, String texteOui) {
        ButtonType oui = new ButtonType(texteOui, ButtonBar.ButtonData.OK_DONE);
        ButtonType non = new ButtonType("Non, je garde", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert alerte = new Alert(Alert.AlertType.WARNING, question, oui, non);
        preparer(alerte, titre);
        alerte.getDialogPane().setMinWidth(540);

        Button boutonOui = (Button) alerte.getDialogPane().lookupButton(oui);
        Button boutonNon = (Button) alerte.getDialogPane().lookupButton(non);
        boutonOui.getStyleClass().addAll("btn", "btn-danger");
        boutonNon.getStyleClass().addAll("btn", "btn-succes");
        boutonOui.setDefaultButton(false);
        boutonNon.setDefaultButton(true);

        return alerte.showAndWait().filter(r -> r == oui).isPresent();
    }

    private void preparer(Dialog<?> dialog, String titre) {
        dialog.setTitle(titre);
        dialog.setHeaderText(null);
        dialog.setGraphic(null);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource(FEUILLE_STYLE).toExternalForm());
        dialog.initOwner(fenetreActive());
    }

    private Window fenetreActive() {
        return Window.getWindows().stream()
                .filter(Window::isShowing)
                .filter(Window::isFocused)
                .findFirst()
                .orElse(racine.getScene().getWindow());
    }

    private VBox groupe(String question, Node champ, String aide) {
        Label q = new Label(question);
        q.getStyleClass().add("question-champ");
        q.setWrapText(true);
        VBox boite = new VBox(6, q, champ);
        if (aide != null) {
            Label a = new Label(aide);
            a.getStyleClass().add("aide-champ");
            a.setWrapText(true);
            boite.getChildren().add(a);
        }
        return boite;
    }

    private TextField champ(String exemple) {
        TextField t = new TextField();
        t.setPromptText(exemple);
        return t;
    }

    private Button bouton(String texte, String... classes) {
        Button b = new Button(texte);
        b.getStyleClass().add("btn");
        b.getStyleClass().addAll(classes);
        return b;
    }

    private Label creerBadge(int niveau) {
        String texte = switch (niveau) {
            case OK -> "✔  Tout va bien";
            case PREVOIR -> "⏰  À prévoir bientôt";
            case URGENT -> "⚠  À faire maintenant";
            case INCONNU -> "Pas encore d'information";
            default -> "Aucun entretien surveillé";
        };
        Label badge = new Label(texte);
        badge.getStyleClass().addAll("badge", "badge-" + suffixe(niveau));
        return badge;
    }

    /** Petit message qui apparaît en bas de l'écran puis disparaît tout seul. */
    private void toast(String message) {
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

    private void montrer(Node n, boolean visible) {
        n.setVisible(visible);
        n.setManaged(visible);
    }

    /** Analyse un véhicule ; les entretiens les plus urgents sont placés en premier. */
    private List<CalculateurEntretien.Bilan> analyser(Vehicule v) {
        List<Entretien> historique = EntretienDB.getEntretiensPourVehicule(v.getId());
        List<Preconisation> regles = PreconisationDB.getToutesLesPreconisations();
        List<CalculateurEntretien.Bilan> bilans =
                new ArrayList<>(CalculateurEntretien.analyserEntretien(v, historique, regles));
        bilans.sort(Comparator.comparingDouble((CalculateurEntretien.Bilan b) -> b.pourcentage).reversed());
        return bilans;
    }

    private int niveauDe(double pourcentage) {
        if (pourcentage >= 100) return URGENT;
        if (pourcentage >= 80) return PREVOIR;
        if (pourcentage > 0) return OK;
        return INCONNU;
    }

    private int niveauGlobal(List<CalculateurEntretien.Bilan> bilans) {
        if (bilans.isEmpty()) return AUCUNE_REGLE;
        boolean urgent = false, prevoir = false, ok = false;
        for (CalculateurEntretien.Bilan b : bilans) {
            int n = niveauDe(b.pourcentage);
            if (n == URGENT) urgent = true;
            else if (n == PREVOIR) prevoir = true;
            else if (n == OK) ok = true;
        }
        if (urgent) return URGENT;
        if (prevoir) return PREVOIR;
        if (ok) return OK;
        return INCONNU;
    }

    private String suffixe(int niveau) {
        return switch (niveau) {
            case OK -> "ok";
            case PREVOIR -> "prevoir";
            case URGENT -> "urgent";
            default -> "neutre";
        };
    }

    private static String sansEspaces(String s) {
        return s == null ? "" : s.replaceAll("[\\s\\u00A0\\u202F]", "");
    }

    private static boolean estEntier(String s) {
        return sansEspaces(s).matches("\\d{1,9}");
    }

    private static int parseEntier(String s) {
        return Integer.parseInt(sansEspaces(s));
    }

    private static boolean estMontant(String s) {
        String propre = sansEspaces(s).replace("€", "").replace(',', '.');
        return propre.isEmpty() || propre.matches("\\d{1,7}(\\.\\d{1,2})?");
    }

    private static double parseMontant(String s) {
        String propre = sansEspaces(s).replace("€", "").replace(',', '.');
        return propre.isEmpty() ? 0 : Double.parseDouble(propre);
    }

    /** 45000 -> "45 000" */
    private static String km(int valeur) {
        return String.format(Locale.FRANCE, "%,d", valeur).replace('\u202F', '\u00A0');
    }
}
