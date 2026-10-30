package com.carnetentretien.carnetentretien;

import com.carnetentretien.carnetentretien.service.AppPaths;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

/**
 * Base des tests qui touchent au disque : base SQLite, images et config.properties sont placés dans un
 * dossier temporaire, jamais dans les vraies données de l'utilisateur.
 */
public abstract class BaseDonneesTest {

    @TempDir
    protected Path dossier;

    private String ancienneValeur;

    @BeforeEach
    void pointerVersDossierTemporaire() {
        ancienneValeur = System.getProperty(AppPaths.PROPRIETE_DOSSIER);
        System.setProperty(AppPaths.PROPRIETE_DOSSIER, dossier.toString());
    }

    @AfterEach
    void restaurerDossier() {
        if (ancienneValeur == null) {
            System.clearProperty(AppPaths.PROPRIETE_DOSSIER);
        } else {
            System.setProperty(AppPaths.PROPRIETE_DOSSIER, ancienneValeur);
        }
    }
}
