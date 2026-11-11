package com.carnetentretien.carnetentretien.service;

import com.carnetentretien.carnetentretien.model.ControleTechnique;
import com.carnetentretien.carnetentretien.model.Entretien;
import com.carnetentretien.carnetentretien.model.EntretienPlanifie;
import com.carnetentretien.carnetentretien.model.Preconisation;
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

    private String ecrireEtLire(Path dossier, List<Entretien> historique,
                                List<EntretienPlanifie> planifies,
                                List<ControleTechnique> controles) throws IOException {
        File fichier = dossier.resolve("export.csv").toFile();
        List<Preconisation> regles = List.of(new Preconisation(1, "Vidange", 15000, 12));
        List<CalculateurEntretien.Bilan> bilans = CalculateurEntretien.analyserEntretien(peugeot, historique, regles);
        ControleTechniqueService.Suivi suiviCt = ControleTechniqueService.analyser(controles);
        CsvHistorique.ecrire(fichier, peugeot, historique, planifies, controles, bilans, suiviCt);
        return Files.readString(fichier.toPath(), StandardCharsets.UTF_8);
    }

    @Test
    void leFichierCommenceParUneMarqueUtf8PourExcel(@TempDir Path dossier) throws IOException {
        String contenu = ecrireEtLire(dossier, List.of(), List.of(), List.of());
        assertTrue(contenu.startsWith("\ufeff"));
    }

    @Test
    void leFichierContientLesInfosVehiculeEtToutesLesSections(@TempDir Path dossier) throws IOException {
        List<Entretien> historique = List.of(
                new Entretien(1, 1, "Vidange", LocalDate.of(2026, 3, 15), 52000, 80.5),
                new Entretien(2, 1, "Freins", LocalDate.of(2026, 4, 2), 53000, 120.25));
        List<EntretienPlanifie> planifies = List.of(
                new EntretienPlanifie(1, 1, "Pneus avant", LocalDate.of(2026, 12, 1), "Michelin"));
        List<ControleTechnique> controles = List.of(
                new ControleTechnique(1, 1, LocalDate.of(2025, 5, 10),
                        ControleTechnique.Resultat.FAVORABLE, LocalDate.of(2027, 5, 10), "RAS"));

        String contenu = ecrireEtLire(dossier, historique, planifies, controles);

        assertTrue(contenu.contains("Véhicule :;Peugeot 208"));
        assertTrue(contenu.contains("Immatriculation :;AB-123-CD"));
        assertTrue(contenu.contains("ENTRETIENS RÉALISÉS"));
        assertTrue(contenu.contains("2026-04-02;Freins;53000;120.25"));
        assertTrue(contenu.contains("2026-03-15;Vidange;52000;80.5"));
        assertTrue(contenu.contains(";;Total :;200.75"));

        assertTrue(contenu.contains("ENTRETIENS PLANIFIÉS"));
        assertTrue(contenu.contains("2026-12-01;Pneus avant;Michelin"));

        assertTrue(contenu.contains("CONTRÔLES TECHNIQUES"));
        assertTrue(contenu.contains("2025-05-10;Favorable;2027-05-10;RAS"));

        assertTrue(contenu.contains("À SURVEILLER"));
        assertTrue(contenu.contains("Entretien;Vidange;"));
        assertTrue(contenu.contains("Contrôle technique;Contrôle technique;2027-05-10;"));
    }

    @Test
    void unExportSansDonneesAfficheLesSectionsVides(@TempDir Path dossier) throws IOException {
        String contenu = ecrireEtLire(dossier, List.of(), List.of(), List.of());

        assertTrue(contenu.contains(";;Total :;0.0"));
        assertTrue(contenu.contains("ENTRETIENS PLANIFIÉS"));
        assertTrue(contenu.contains("CONTRÔLES TECHNIQUES"));
        long aucuns = contenu.lines().filter("Aucun"::equals).count();
        assertTrue(aucuns >= 2);
    }

    @Test
    void ecrireDansUnDossierInexistantLeveUneIOException(@TempDir Path dossier) {
        File impossible = dossier.resolve("n-existe-pas").resolve("export.csv").toFile();
        ControleTechniqueService.Suivi suiviCt = ControleTechniqueService.analyser(List.of());
        assertThrows(IOException.class, () -> CsvHistorique.ecrire(impossible, peugeot,
                List.of(), List.of(), List.of(), List.of(), suiviCt));
    }
}
