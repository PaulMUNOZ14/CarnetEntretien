package com.carnetentretien.carnetentretien.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AppPathsTest {

    private String ancienneValeur;

    @BeforeEach
    void sauvegarder() {
        ancienneValeur = System.getProperty(AppPaths.PROPRIETE_DOSSIER);
    }

    @AfterEach
    void restaurer() {
        if (ancienneValeur == null) {
            System.clearProperty(AppPaths.PROPRIETE_DOSSIER);
        } else {
            System.setProperty(AppPaths.PROPRIETE_DOSSIER, ancienneValeur);
        }
    }

    @Test
    void laProprieteSystemeImposeLeDossier(@TempDir Path courant, @TempDir Path perso) throws IOException {
        Files.createFile(courant.resolve("carnet_entretien.db"));   // même avec des données ici, la propriété gagne
        Path voulu = courant.resolve("ailleurs");

        assertEquals(voulu.toAbsolutePath().normalize(), AppPaths.choisirDossier(voulu.toString(), courant, perso));
    }

    @Test
    void uneProprieteVideEstIgnoree(@TempDir Path courant, @TempDir Path perso) {
        assertEquals(perso.resolve("CarnetEntretien").toAbsolutePath().normalize(),
                AppPaths.choisirDossier("  ", courant, perso));
    }

    @Test
    void leDossierCourantEstGardeSiIlContientDejaLaBase(@TempDir Path courant, @TempDir Path perso) throws IOException {
        Files.createFile(courant.resolve("carnet_entretien.db"));

        assertEquals(courant.normalize(), AppPaths.choisirDossier(null, courant, perso));
    }

    @Test
    void leDossierCourantEstGardeSiIlContientDejaLaConfiguration(@TempDir Path courant, @TempDir Path perso) throws IOException {
        Files.createFile(courant.resolve("config.properties"));

        assertEquals(courant.normalize(), AppPaths.choisirDossier(null, courant, perso));
    }

    @Test
    void sinonLesDonneesVontDansLeDossierPersonnelDeLUtilisateur(@TempDir Path courant, @TempDir Path perso) {
        assertEquals(perso.resolve("CarnetEntretien").toAbsolutePath().normalize(),
                AppPaths.choisirDossier(null, courant, perso));
    }

    @Test
    void tousLesCheminsDecoulentDuDossierDeDonnees(@TempDir Path temp) {
        System.setProperty(AppPaths.PROPRIETE_DOSSIER, temp.toString());

        assertEquals(temp, AppPaths.dossierDonnees());
        assertEquals(temp.resolve("carnet_entretien.db"), AppPaths.fichierBaseDeDonnees());
        assertEquals(temp.resolve("images"), AppPaths.dossierImages());
        assertEquals(temp.resolve("config.properties"), AppPaths.fichierConfiguration());
    }
}
