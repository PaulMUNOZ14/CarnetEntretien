package com.carnetentretien.carnetentretien.service;

import com.carnetentretien.carnetentretien.model.Entretien;
import com.carnetentretien.carnetentretien.model.Vehicule;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/** Écriture du fichier CSV de l'historique (séparée de la fenêtre « Enregistrer sous » pour pouvoir être testée). */
public final class CsvHistorique {

    private CsvHistorique() {
    }

    public static void ecrire(File fichier, Vehicule vehicule, List<Entretien> historique) throws IOException {
        try (PrintWriter writer = new PrintWriter(fichier, "UTF-8")) {
            writer.write('﻿');
            writer.println("Véhicule :;" + vehicule.getMarque() + " " + vehicule.getModele());
            writer.println("Date;Opération;Kilométrage;Coût (€)");

            double coutTotal = 0;
            for (Entretien e : historique) {
                writer.println(e.getDateRealisation() + ";" + e.getTypeOperation() + ";" + e.getKilometrageRealisation() + ";" + e.getCout());
                coutTotal += e.getCout();
            }
            writer.println(";;Total :;" + coutTotal);
        }
    }
}
