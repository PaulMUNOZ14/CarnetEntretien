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

    public static final String SITE_API = "https://apiplaqueimmatriculation.com";
    public static final String LIEN_OBTENIR_CLE = SITE_API + "/obtenir_une_cle_api/";

    static final String CLE_API = "plaque.api.token";
    static final String AIDE_AFFICHEE = "aide.cle.affichee";

    private ConfigService() {
    }

    /** @return la clé enregistrée dans config.properties, ou vide s'il n'y en a pas */
    public static Optional<String> getCleApi() {
        String valeur = charger().getProperty(CLE_API);
        return (valeur == null || valeur.isBlank()) ? Optional.empty() : Optional.of(valeur.trim());
    }

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

    public static boolean aideDejaAffichee() {
        return Boolean.parseBoolean(charger().getProperty(AIDE_AFFICHEE, "false"));
    }

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

    private static Properties charger() {
        Properties p = new Properties();
        Path fichier = AppPaths.fichierConfiguration();
        if (Files.isRegularFile(fichier)) {
            try (Reader in = Files.newBufferedReader(fichier, StandardCharsets.UTF_8)) {
                p.load(in);
            } catch (IOException e) {
                System.out.println("Erreur de lecture de la configuration : " + e.getMessage());
            }
        }
        return p;
    }

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
            System.out.println("Erreur d'écriture de la configuration : " + e.getMessage());
        }
    }
}
