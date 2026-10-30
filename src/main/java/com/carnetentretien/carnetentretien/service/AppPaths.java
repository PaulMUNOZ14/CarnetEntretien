package com.carnetentretien.carnetentretien.service;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Emplacements des données de l'application : base SQLite, images des véhicules, config.properties.
 * <p>
 * Ordre de choix du dossier :
 * <ol>
 *   <li>la propriété système {@code carnet.data.dir} si elle est définie (tests, installeur) ;</li>
 *   <li>le dossier courant s'il contient déjà {@code carnet_entretien.db} ou {@code config.properties}
 *       (cas du développement : on retrouve les données existantes) ;</li>
 *   <li>sinon {@code <dossier personnel>/CarnetEntretien}, qui est toujours modifiable, contrairement au
 *       dossier d'installation d'un exécutable (par ex. « Program Files »).</li>
 * </ol>
 */
public final class AppPaths {

    public static final String PROPRIETE_DOSSIER = "carnet.data.dir";
    static final String NOM_BASE = "carnet_entretien.db";
    static final String NOM_CONFIG = "config.properties";

    private AppPaths() {
    }

    public static Path dossierDonnees() {
        return choisirDossier(System.getProperty(PROPRIETE_DOSSIER),
                Path.of("").toAbsolutePath(),
                Path.of(System.getProperty("user.home", ".")));
    }

    static Path choisirDossier(String propriete, Path courant, Path personnel) {
        if (propriete != null && !propriete.isBlank()) {
            return Path.of(propriete).toAbsolutePath().normalize();
        }
        if (Files.isRegularFile(courant.resolve(NOM_BASE)) || Files.isRegularFile(courant.resolve(NOM_CONFIG))) {
            return courant.normalize();
        }
        return personnel.resolve("CarnetEntretien").toAbsolutePath().normalize();
    }

    public static Path fichierBaseDeDonnees() {
        return dossierDonnees().resolve(NOM_BASE);
    }

    public static Path dossierImages() {
        return dossierDonnees().resolve("images");
    }

    public static Path fichierConfiguration() {
        return dossierDonnees().resolve(NOM_CONFIG);
    }
}
