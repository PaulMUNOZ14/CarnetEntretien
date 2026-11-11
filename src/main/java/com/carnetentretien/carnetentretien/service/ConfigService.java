package com.carnetentretien.carnetentretien.service;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Optional;
import java.util.Properties;

/**
 * Réglages de l'utilisateur, dans {@code config.properties} : clé API (même clé {@code plaque.api.token}
 * qu'avant, les installations existantes continuent donc de fonctionner) et mémoire de la fenêtre d'aide.
 * Le fichier est relu à chaque appel : rien n'est gardé en mémoire.
 */
public final class ConfigService {

    /** Adresse du site du service de recherche par plaque. */
    public static final String SITE_API = "https://apiplaqueimmatriculation.com";

    /** Page du site où l'utilisateur obtient sa clé personnelle. */
    public static final String LIEN_OBTENIR_CLE = SITE_API + "/obtenir_une_cle_api/";

    /** Nom de la propriété contenant la clé API*/
    static final String CLE_API = "plaque.api.token";

    /** Propriété qui retient que la fenêtre d'explication de la clé a déjà été montrée au premier lancement. */
    static final String AIDE_AFFICHEE = "aide.cle.affichee";

    /** @return la clé enregistrée dans config.properties, ou vide s'il n'y en a pas */
    public static Optional<String> getCleApi() {
        String valeur = charger().getProperty(CLE_API);
        return (valeur == null || valeur.isBlank()) ? Optional.empty() : Optional.of(valeur.trim());
    }

    /** @return {@code true} si une clé personnelle est enregistrée */
    public static boolean aCleApi() {
        return getCleApi().isPresent();
    }

    /** Enregistre la clé (espaces autour retirés). Une valeur vide la supprime. */
    public static void setCleApi(String cle) {
        Properties p = charger();
        if (cle == null || cle.isBlank()) {
            p.remove(CLE_API);
        } else {
            p.setProperty(CLE_API, cle.trim());
        }
        sauvegarder(p);
    }

    /** @return {@code true} si la fenêtre d'aide de la clé a déjà été affichée (jamais réaffichée automatiquement) */
    public static boolean aideDejaAffichee() {
        return Boolean.parseBoolean(charger().getProperty(AIDE_AFFICHEE, "false"));
    }

    /** Mémorise que la fenêtre d'aide a été affichée, sans toucher à la clé. */
    public static void marquerAideAffichee() {
        Properties p = charger();
        p.setProperty(AIDE_AFFICHEE, "true");
        sauvegarder(p);
    }

    /** Clé masquée pour l'affichage : seuls les 4 derniers caractères restent visibles. */
    public static String masquerCle(String cle) {
        if (cle == null || cle.isBlank()) {
            return "";
        }
        String propre = cle.trim();
        if (propre.length() <= 8) {
            return "•".repeat(propre.length());
        }
        return "•".repeat(propre.length() - 4) + propre.substring(propre.length() - 4);
    }

    /** Lit {@code config.properties} ; un fichier absent ou illisible donne des propriétés vides. */
    private static Properties charger() {
        Properties p = new Properties();
        Path fichier = AppPaths.fichierConfiguration();
        if (Files.isRegularFile(fichier)) {
            try (Reader in = Files.newBufferedReader(fichier, StandardCharsets.UTF_8)) {
                p.load(in);
            } catch (IOException e) {
                System.err.println("Erreur de lecture de la configuration : " + e.getMessage());
            }
        }
        return p;
    }

    /** Écrit {@code config.properties} (en créant le dossier au besoin), en lecture/écriture pour l'utilisateur seul quand le système le permet. */
    private static void sauvegarder(Properties p) {
        Path fichier = AppPaths.fichierConfiguration();
        try {
            Files.createDirectories(fichier.getParent());
            try (Writer out = Files.newBufferedWriter(fichier, StandardCharsets.UTF_8)) {
                p.store(out, "Carnet Entretien - configuration");
            }
            try {
                Files.setPosixFilePermissions(fichier, PosixFilePermissions.fromString("rw-------"));
            } catch (UnsupportedOperationException ignore) {
                // Windows : pas de permissions POSIX
            }
        } catch (IOException e) {
            System.err.println("Erreur d'écriture de la configuration : " + e.getMessage());
        }
    }
}
