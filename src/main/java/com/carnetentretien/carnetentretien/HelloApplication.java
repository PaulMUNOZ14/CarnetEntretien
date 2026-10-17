package com.carnetentretien.carnetentretien;

import com.carnetentretien.carnetentretien.db.DatabaseManager;
import com.carnetentretien.carnetentretien.db.VehiculeDB;
import com.carnetentretien.carnetentretien.model.Vehicule;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        DatabaseManager.initDatabase();

        // Test
        /*
        if (VehiculeDB.getTousLesVehicules().isEmpty()) {
            Vehicule maVoiture = new Vehicule(0, "Citroën", "C4 Coupé", 185000, 15000);
            VehiculeDB.ajouterVehicule(maVoiture);
        }

        System.out.println("--- VÉHICULES DANS LA BASE ---");
        for (Vehicule v : VehiculeDB.getTousLesVehicules()) {
            System.out.println("-> " + v.getMarque() + " " + v.getModele() + " | " + v.getKilometrageActuel() + " km");
        }
        System.out.println("------------------------------");
        */

        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);
        stage.setTitle("Carnet d'Entretien");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}