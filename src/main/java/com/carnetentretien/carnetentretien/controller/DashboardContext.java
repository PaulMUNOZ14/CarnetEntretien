package com.carnetentretien.carnetentretien.controller;

import com.carnetentretien.carnetentretien.model.Vehicule;
import javafx.geometry.Rectangle2D;
import javafx.stage.Window;

/**
 * Services rendus par la fenêtre principale aux contrôleurs des écrans et des fenêtres de saisie.
 * <p>
 * Cela évite que chaque contrôleur ait besoin de connaître {@link DashboardController} en entier :
 * il lui suffit de demander la navigation, un message de confirmation ou la taille de l'écran.
 */
public interface DashboardContext {

    /** Revient à la liste des véhicules. */
    void afficherAccueil();

    /** Ouvre l'écran d'un véhicule. */
    void ouvrirVehicule(Vehicule vehicule);

    /** Ouvre le calendrier de tous les véhicules. */
    void ouvrirCalendrier();

    /** Ouvre le calendrier filtré sur un véhicule. */
    void ouvrirCalendrier(Vehicule vehicule);

    /** Réaffiche l'écran courant (accueil, véhicule ou calendrier) après une modification. */
    void rafraichir();

    /** Affiche un court message en bas de la fenêtre. */
    void toast(String message);

    /** @return le véhicule actuellement ouvert, ou {@code null} sur l'accueil */
    Vehicule vehiculeSelectionne();

    /** Mémorise le véhicule actuellement ouvert (par exemple après une modification). */
    void definirVehiculeSelectionne(Vehicule vehicule);

    /** @return la fenêtre active (celle qui doit posséder les boîtes de dialogue) */
    Window fenetreActive();

    /** @return la zone utilisable (hors barre des tâches) de l'écran où se trouve la fenêtre */
    Rectangle2D zoneEcran();

    /** @return la largeur voulue pour une fenêtre de saisie, réduite si l'écran est petit */
    double largeurDialogue(double voulue);

    /** @return la hauteur maximale du contenu défilant d'une fenêtre de saisie */
    double hauteurMaxDialogue();

    /** @return {@code true} quand la fenêtre principale est étroite (mode compact) */
    boolean estCompact();

    /** @return {@code true} quand l'écran est petit (largeur &lt; 1300 px ou hauteur &lt; 800 px) */
    boolean petitEcran();
}
