package com.carnetentretien.carnetentretien;

import com.carnetentretien.carnetentretien.db.DatabaseManager;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import java.io.IOException;

public class HelloApplication extends Application {

    private static HostServices services;

    /** Ouvre une adresse web dans le navigateur de l'utilisateur. */
    public static void ouvrirLien(String url) {
        if (services != null) services.showDocument(url);
    }

    @Override
    public void start(Stage stage) throws IOException {
        services = getHostServices();
        DatabaseManager.initDatabase();

        Rectangle2D ecran = Screen.getPrimary().getVisualBounds();
        double largeur = Math.min(1100, ecran.getWidth() * 0.95);
        double hauteur = Math.min(750, ecran.getHeight() * 0.92);

        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), largeur, hauteur);
        stage.setTitle("Carnet d'Entretien");
        stage.setMinWidth(Math.min(360, ecran.getWidth()));
        stage.setMinHeight(Math.min(480, ecran.getHeight()));
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
