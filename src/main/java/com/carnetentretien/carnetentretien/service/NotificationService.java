package com.carnetentretien.carnetentretien.service;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.AWTException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;

/**
 * Notifications Windows de Carnet Entretien, affichées par l'intermédiaire de la zone de
 * notification du système (icône de la barre des tâches).
 * <p>
 * Un contrôle a lieu quelques secondes après le démarrage, puis toutes les six heures. Une
 * notification n'est affichée qu'une seule fois par jour : les échéances d'entretien, les
 * contrôles techniques et les planifications proches sont rassemblés par
 * {@link RappelNotification}. L'icône propose aussi un menu pour ouvrir l'application,
 * vérifier les échéances ou quitter.
 * <p>
 * L'application doit rester ouverte (ou réduite) pour que les rappels continuent.
 */
public final class NotificationService {

    /** Ressource utilisée comme icône dans la zone de notification. */
    private static final String RESSOURCE_ICONE = "/com/carnetentretien/carnetentretien/icone.png";

    /** Délai laissé à la fenêtre principale avant le premier contrôle. */
    private static final Duration PREMIER_CONTROLE = Duration.seconds(5);

    /** Intervalle entre deux contrôles automatiques. */
    private static final Duration INTERVALLE_CONTROLE = Duration.hours(6);

    private static Stage fenetre;
    private static SystemTray zoneNotification;
    private static TrayIcon icone;
    private static PauseTransition premierControle;
    private static Timeline controlesPeriodiques;

    private NotificationService() {
    }

    /**
     * @return {@code true} si le système accepte les icônes et notifications de la zone de notification
     */
    public static boolean estSupportee() {
        try {
            return !GraphicsEnvironment.isHeadless() && SystemTray.isSupported();
        } catch (UnsupportedOperationException | SecurityException e) {
            return false;
        }
    }

    /**
     * Démarre les notifications pour la fenêtre principale. Sans effet si l'utilisateur les a
     * désactivées ou si le système ne les prend pas en charge.
     *
     * @param stage fenêtre principale, ramenée au premier plan quand l'utilisateur clique sur l'icône
     */
    public static void demarrer(Stage stage) {
        fenetre = stage;
        appliquerReglage();
    }

    /**
     * Applique le réglage enregistré : installe ou retire l'icône et la surveillance périodique.
     * Appelée au démarrage et après chaque changement dans les réglages.
     */
    public static void appliquerReglage() {
        if (ConfigService.notificationsActives() && installerIcone()) {
            demarrerSurveillance();
        } else {
            arreter();
        }
    }

    /**
     * Arrête la surveillance et retire l'icône de la zone de notification.
     * Utilisée quand l'utilisateur désactive les notifications et à la fermeture de l'application.
     */
    public static void arreter() {
        arreterSurveillance();
        if (icone != null && zoneNotification != null) {
            zoneNotification.remove(icone);
        }
        icone = null;
        zoneNotification = null;
    }

    /**
     * Affiche immédiatement une notification de démonstration, sans modifier la date du dernier
     * rappel automatique.
     *
     * @return {@code true} si la notification a pu être affichée
     */
    public static boolean tester() {
        if (!installerIcone()) return false;
        afficher("Carnet Entretien",
                "Les notifications Windows fonctionnent : un rappel apparaîtra ici quand une échéance approchera.",
                TrayIcon.MessageType.INFO);
        return true;
    }

    /**
     * Vérifie les échéances et affiche un rappel si nécessaire.
     *
     * @param manuel {@code true} pour un contrôle demandé depuis le menu de l'icône : le message
     *               « tout est à jour » est alors affiché même sans échéance, et la limite d'un
     *               rappel par jour est ignorée
     */
    public static void verifier(boolean manuel) {
        if (!manuel && !ConfigService.notificationsActives()) return;
        if (!manuel && ConfigService.derniereNotification().filter(LocalDate.now()::equals).isPresent()) {
            return;
        }

        List<RappelNotification> rappels;
        try {
            rappels = RappelNotification.collecter(CalendrierService.charger(null), LocalDate.now());
        } catch (RuntimeException e) {
            System.err.println("Impossible de vérifier les échéances : " + e.getMessage());
            return;
        }

        if (rappels.isEmpty()) {
            if (icone != null) icone.setToolTip("Carnet Entretien — rappels d'entretien");
            if (manuel && installerIcone()) {
                afficher("Carnet Entretien", "Aucune échéance à signaler : tout est à jour.",
                        TrayIcon.MessageType.INFO);
            }
            return;
        }

        if (!installerIcone()) return;

        boolean enRetard = rappels.stream().anyMatch(RappelNotification::isEnRetard);
        afficher(RappelNotification.titre(rappels), RappelNotification.texte(rappels),
                enRetard ? TrayIcon.MessageType.WARNING : TrayIcon.MessageType.INFO);
        ConfigService.setDerniereNotification(LocalDate.now());
        icone.setToolTip("Carnet Entretien — " + rappels.size()
                + (rappels.size() > 1 ? " rappels en attente" : " rappel en attente"));
    }

    // ---------- Zone de notification ----------

    private static boolean installerIcone() {
        if (icone != null) return true;
        if (!estSupportee()) return false;

        try {
            zoneNotification = SystemTray.getSystemTray();
            Image image = chargerIcone(zoneNotification.getTrayIconSize());

            PopupMenu menu = new PopupMenu();

            MenuItem ouvrir = new MenuItem("Ouvrir Carnet Entretien");
            ouvrir.addActionListener(e -> ouvrirFenetre());
            menu.add(ouvrir);

            MenuItem verifier = new MenuItem("Vérifier les échéances");
            verifier.addActionListener(e -> Platform.runLater(() -> verifier(true)));
            menu.add(verifier);

            menu.addSeparator();

            MenuItem desactiver = new MenuItem("Désactiver les notifications");
            desactiver.addActionListener(e -> Platform.runLater(() -> {
                ConfigService.setNotificationsActives(false);
                arreter();
            }));
            menu.add(desactiver);

            menu.addSeparator();

            MenuItem quitter = new MenuItem("Quitter");
            quitter.addActionListener(e -> Platform.runLater(() -> {
                arreter();
                Platform.exit();
            }));
            menu.add(quitter);

            icone = new TrayIcon(image, "Carnet Entretien — rappels d'entretien", menu);
            icone.setImageAutoSize(true);
            icone.addActionListener(e -> ouvrirFenetre());
            zoneNotification.add(icone);
            return true;
        } catch (AWTException | UnsupportedOperationException | SecurityException e) {
            System.err.println("Notifications Windows indisponibles : " + e.getMessage());
            icone = null;
            zoneNotification = null;
            return false;
        }
    }

    private static void afficher(String titre, String texte, TrayIcon.MessageType type) {
        if (icone == null) return;
        icone.displayMessage(titre, texte, type);
    }

    private static void ouvrirFenetre() {
        Stage stage = fenetre;
        if (stage == null) return;
        Platform.runLater(() -> {
            if (!stage.isShowing()) stage.show();
            if (stage.isIconified()) stage.setIconified(false);
            stage.toFront();
            stage.requestFocus();
        });
    }

    private static void demarrerSurveillance() {
        if (premierControle != null || controlesPeriodiques != null) return;

        premierControle = new PauseTransition(PREMIER_CONTROLE);
        premierControle.setOnFinished(e -> verifier(false));
        premierControle.play();

        controlesPeriodiques = new Timeline(new KeyFrame(INTERVALLE_CONTROLE, e -> verifier(false)));
        controlesPeriodiques.setCycleCount(Animation.INDEFINITE);
        controlesPeriodiques.play();
    }

    private static void arreterSurveillance() {
        if (premierControle != null) {
            premierControle.stop();
            premierControle = null;
        }
        if (controlesPeriodiques != null) {
            controlesPeriodiques.stop();
            controlesPeriodiques = null;
        }
    }

    /** Charge l'icône de l'application et la redimensionne pour la barre des tâches. */
    private static Image chargerIcone(Dimension taille) {
        int cote = Math.max(16, Math.max(taille.width, taille.height));
        try (InputStream entree = NotificationService.class.getResourceAsStream(RESSOURCE_ICONE)) {
            if (entree != null) {
                BufferedImage source = ImageIO.read(entree);
                if (source != null) return redimensionner(source, cote);
            }
        } catch (IOException e) {
            System.err.println("Icône de notification illisible : " + e.getMessage());
        }
        return iconeParDefaut(cote);
    }

    private static Image redimensionner(BufferedImage source, int cote) {
        BufferedImage image = new BufferedImage(cote, cote, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(source, 0, 0, cote, cote, null);
        } finally {
            g.dispose();
        }
        return image;
    }

    /** Icône de secours si le fichier de l'application est absent ou illisible. */
    private static Image iconeParDefaut(int cote) {
        BufferedImage image = new BufferedImage(cote, cote, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(0x1D5FD1));
            g.fillRoundRect(0, 0, cote, cote, cote / 3, cote / 3);
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(9, cote - 6)));
            FontMetrics mesures = g.getFontMetrics();
            String lettre = "C";
            g.drawString(lettre, (cote - mesures.stringWidth(lettre)) / 2,
                    (cote - mesures.getHeight()) / 2 + mesures.getAscent());
        } finally {
            g.dispose();
        }
        return image;
    }
}
