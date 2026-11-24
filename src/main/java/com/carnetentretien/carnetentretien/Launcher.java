package com.carnetentretien.carnetentretien;

import com.carnetentretien.carnetentretien.service.IdentiteWindows;
import javafx.application.Application;

/**
 * Lanceur de l'application. Il ne dérive pas de {@link javafx.application.Application} :
 * cela permet de démarrer JavaFX depuis un jar ou un exécutable sans erreur
 * « modules JavaFX manquants », et c'est la classe à exécuter depuis l'IDE.
 */
public class Launcher {
    /** Déclare le nom de l'application à Windows, puis démarre {@link HelloApplication}. */
    public static void main(String[] args) {
        IdentiteWindows.appliquer();
        Application.launch(HelloApplication.class, args);
    }
}
