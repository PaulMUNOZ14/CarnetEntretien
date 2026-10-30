package com.carnetentretien.carnetentretien.service;

import com.carnetentretien.carnetentretien.model.Entretien;
import com.carnetentretien.carnetentretien.model.Vehicule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvHistoriqueTest {

    private final Vehicule peugeot = new Vehicule(1, "AB-123-CD", "Peugeot", "208", 50000, 12000);

    private String ecrireEtLire(Path dossier, List<Entretien> historique) throws IOException {
        File fichier = dossier.resolve("export.csv").toFile();
        CsvHistorique.ecrire(fichier, peugeot, historique);
        return new String(Files.readAllBytes(fichier.toPath()), StandardCharsets.UTF_8);
    }

    @Test
    void leFichierCommenceParUneMarqueUtf8PourExcel(@TempDir Path dossier) throws IOException {
        String contenu = ecrireEtLire(dossier, List.of());

        assertTrue(contenu.startsWith("﻿"));
    }

    @Test
    void leFichierContientLEnteteLeVehiculeLesLignesEtLeTotal(@TempDir Path dossier) throws IOException {
        List<Entretien> historique = List.of(
                new Entretien(1, 1, "Vidange", LocalDate.of(2026, 3, 15), 52000, 80.5),
                new Entretien(2, 1, "Freins", LocalDate.of(2026, 4, 2), 53000, 120.25));

        String[] lignes = ecrireEtLire(dossier, historique).substring(1).split("\\R");

        assertEquals(5, lignes.length);
        assertEquals("Véhicule :;Peugeot 208", lignes[0]);
        assertEquals("Date;Opération;Kilométrage;Coût (€)", lignes[1]);
        assertEquals("2026-03-15;Vidange;52000;80.5", lignes[2]);
        assertEquals("2026-04-02;Freins;53000;120.25", lignes[3]);
        assertEquals(";;Total :;200.75", lignes[4]);
    }

    @Test
    void unHistoriqueVideDonneUnTotalANul(@TempDir Path dossier) throws IOException {
        String[] lignes = ecrireEtLire(dossier, List.of()).substring(1).split("\\R");

        assertEquals(3, lignes.length);
        assertEquals(";;Total :;0.0", lignes[2]);
    }

    @Test
    void ecrireDansUnDossierInexistantLeveUneIOException(@TempDir Path dossier) {
        File impossible = dossier.resolve("n-existe-pas").resolve("export.csv").toFile();

        assertThrows(IOException.class, () -> CsvHistorique.ecrire(impossible, peugeot, List.of()));
    }
}
