package com.carnetentretien.carnetentretien.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Photos choisies par l'utilisateur sur son ordinateur. Le fichier est COPIÉ dans le dossier
 * {@code images/} de l'application : la photo reste disponible même si l'original est supprimé ou déplacé.
 * <p>
 * La base ne stocke que le nom du fichier copié (pas son chemin), ce qui permet de déplacer le dossier de l'application.
 */
public final class ImageService {

    public static final long TAILLE_MAX_OCTETS = 10L * 1024 * 1024;
    public static final Set<String> EXTENSIONS_ACCEPTEES = Set.of("png", "jpg", "jpeg", "gif", "bmp");

    private ImageService() {
    }

    /** Vérifie qu'un fichier peut être importé, sans rien copier. Le message de l'exception est affichable tel quel. */
    public static void valider(Path source) throws IOException {
        if (source == null || !Files.isRegularFile(source)) {
            throw new IOException("Le fichier choisi est introuvable.");
        }
        String extension = extension(source.getFileName().toString());
        if (!EXTENSIONS_ACCEPTEES.contains(extension)) {
            throw new IOException("Ce format n'est pas pris en charge. Choisissez une image PNG, JPG, GIF ou BMP.");
        }
        if (Files.size(source) > TAILLE_MAX_OCTETS) {
            throw new IOException("Cette image est trop lourde (10 Mo maximum).");
        }
    }

    /**
     * Copie l'image dans le dossier de l'application.
     *
     * @return le nom du fichier copié, à enregistrer en base
     */
    public static String importer(Path source) throws IOException {
        valider(source);

        Path dossier = AppPaths.dossierImages();
        Files.createDirectories(dossier);

        String nom = UUID.randomUUID() + "." + extension(source.getFileName().toString());
        Files.copy(source, dossier.resolve(nom), StandardCopyOption.REPLACE_EXISTING);
        return nom;
    }

    /** @return le fichier correspondant au nom stocké en base, ou null s'il est vide, invalide ou a disparu */
    public static Path resoudre(String nom) {
        Path chemin = cheminSur(nom);
        return (chemin != null && Files.isRegularFile(chemin)) ? chemin : null;
    }

    /** @return l'adresse (file:///…) à donner à un composant Image, ou null si la photo n'existe plus */
    public static String adresse(String nom) {
        Path chemin = resoudre(nom);
        return chemin == null ? null : chemin.toUri().toString();
    }

    /** Supprime la copie. Ne touche jamais au fichier d'origine. @return true si un fichier a été supprimé */
    public static boolean supprimer(String nom) {
        Path chemin = cheminSur(nom);
        if (chemin == null) {
            return false;
        }
        try {
            return Files.deleteIfExists(chemin);
        } catch (IOException e) {
            System.out.println("Impossible de supprimer l'image : " + e.getMessage());
            return false;
        }
    }

    /** Résout le nom dans le dossier d'images en refusant tout ce qui en sortirait (ex. « ../secret.png »). */
    private static Path cheminSur(String nom) {
        if (nom == null || nom.isBlank()) {
            return null;
        }
        Path dossier = AppPaths.dossierImages().normalize();
        Path chemin;
        try {
            chemin = dossier.resolve(nom).normalize();
        } catch (java.nio.file.InvalidPathException e) {
            return null;
        }
        return (chemin.startsWith(dossier) && !chemin.equals(dossier)) ? chemin : null;
    }

    private static String extension(String nomFichier) {
        int point = nomFichier.lastIndexOf('.');
        return point < 0 ? "" : nomFichier.substring(point + 1).toLowerCase(Locale.ROOT);
    }
}
