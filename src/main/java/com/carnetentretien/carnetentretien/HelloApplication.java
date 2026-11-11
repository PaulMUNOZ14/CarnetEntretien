package com.carnetentretien.carnetentretien;

import com.carnetentretien.carnetentretien.db.DatabaseManager;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.Objects;

/**
 * Point d'entrée JavaFX de « Carnet Entretien » : initialise la base de données,
 * charge l'interface principale ({@code dashboard.fxml}) et dimensionne la fenêtre
 * selon la taille de l'écran.
 */
public class HelloApplication extends Application {

    /** Services de l'application, nécessaires pour ouvrir un lien dans le navigateur. Renseigné dans {@link #start}. */
    private static HostServices services;

    /**
     * Ouvre une adresse web dans le navigateur par défaut de l'utilisateur.
     * Ne fait rien si l'application n'est pas encore démarrée.
     *
     * @param url adresse complète, par exemple {@code https://exemple.fr}
     */
    public static void ouvrirLien(String url) {
        if (services != null) services.showDocument(url);
    }

    /**
     * Démarre l'application : crée ou migre la base SQLite, charge l'interface
     * et affiche la fenêtre (taille limitée à 95 % de la largeur et 92 % de la hauteur de l'écran).
     *
     * @param stage fenêtre principale fournie par JavaFX
     * @throws IOException si le fichier FXML ne peut pas être chargé
     */
    @Override
    public void start(Stage stage) throws IOException {
        services = getHostServices();
        DatabaseManager.initDatabase();

        Rectangle2D ecran = Screen.getPrimary().getVisualBounds();
        double largeur = Math.min(1100, ecran.getWidth() * 0.95);
        double hauteur = Math.min(750, ecran.getHeight() * 0.92);

        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("dashboard.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), largeur, hauteur);
        stage.setTitle("Carnet d'Entretien");
        stage.getIcons().add(new Image(Objects.requireNonNull(HelloApplication.class.getResourceAsStream("icone.png"))));
        stage.setMinWidth(Math.min(360, ecran.getWidth()));
        stage.setMinHeight(Math.min(480, ecran.getHeight()));
        stage.setScene(scene);
        stage.show();
    }

    /** Lance l'application (utilisé par l'IDE ; voir aussi {@link Launcher}). */
    public static void main(String[] args) {
        launch();
    }
}
