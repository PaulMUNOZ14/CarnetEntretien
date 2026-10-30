package com.carnetentretien.carnetentretien;

import com.carnetentretien.carnetentretien.db.DatabaseManager;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
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

        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1100, 750);
        stage.setTitle("Carnet d'Entretien");
        stage.setMinWidth(1000);
        stage.setMinHeight(650);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
