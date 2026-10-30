package com.carnetentretien.carnetentretien.service;

import com.carnetentretien.carnetentretien.BaseDonneesTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ImageServiceTest extends BaseDonneesTest {

    private Path creerFichier(String nom, byte[] contenu) throws IOException {
        Path source = Files.createDirectories(dossier.resolve("source")).resolve(nom);
        Files.write(source, contenu);
        return source;
    }

    private boolean dossierImagesVide() throws IOException {
        if (!Files.exists(AppPaths.dossierImages())) return true;
        try (Stream<Path> fichiers = Files.list(AppPaths.dossierImages())) {
            return fichiers.findAny().isEmpty();
        }
    }

    // ---------- importer ----------

    @Test
    void importerCopieLeFichierDansLeDossierImages() throws IOException {
        byte[] contenu = {1, 2, 3, 4, 5};

        String nom = ImageService.importer(creerFichier("ma-voiture.png", contenu));

        Path copie = AppPaths.dossierImages().resolve(nom);
        assertTrue(Files.isRegularFile(copie));
        assertArrayEquals(contenu, Files.readAllBytes(copie));
        assertTrue(nom.endsWith(".png"));
    }

    @Test
    void laPhotoResteDisponibleApresSuppressionDuFichierDOrigine() throws IOException {
        byte[] contenu = {9, 8, 7};
        Path source = creerFichier("photo.jpg", contenu);
        String nom = ImageService.importer(source);

        Files.delete(source);

        Path retrouvee = ImageService.resoudre(nom);
        assertNotNull(retrouvee);
        assertArrayEquals(contenu, Files.readAllBytes(retrouvee));
    }

    @Test
    void deuxImportsDuMemeFichierDonnentDeuxNomsDifferents() throws IOException {
        Path source = creerFichier("photo.png", new byte[]{1});

        String premier = ImageService.importer(source);
        String second = ImageService.importer(source);

        assertNotEquals(premier, second);
        assertNotNull(ImageService.resoudre(premier));
        assertNotNull(ImageService.resoudre(second));
    }

    @Test
    void lExtensionEnMajusculesEstAccepteeEtNormalisee() throws IOException {
        assertTrue(ImageService.importer(creerFichier("PHOTO.JPG", new byte[]{1})).endsWith(".jpg"));
    }

    @Test
    void tousLesFormatsAnnoncesSontAcceptes() throws IOException {
        for (String ext : ImageService.EXTENSIONS_ACCEPTEES) {
            Path source = creerFichier("image." + ext, new byte[]{1});
            assertDoesNotThrow(() -> ImageService.importer(source), "extension " + ext);
        }
    }

    @Test
    void unImportRefuseNeLaissePasDeFichierDansLeDossierImages() throws IOException {
        Path source = creerFichier("document.pdf", new byte[]{1});

        assertThrows(IOException.class, () -> ImageService.importer(source));

        assertTrue(dossierImagesVide());
    }

    // ---------- valider ----------

    @Test
    void unFormatNonSupporteEstRefuseAvecUnMessageClair() throws IOException {
        IOException e = assertThrows(IOException.class, () -> ImageService.valider(creerFichier("document.pdf", new byte[]{1})));

        assertTrue(e.getMessage().contains("format"));
    }

    @Test
    void unFichierSansExtensionEstRefuse() throws IOException {
        assertThrows(IOException.class, () -> ImageService.valider(creerFichier("photo", new byte[]{1})));
    }

    @Test
    void unFichierInexistantEstRefuse() {
        IOException e = assertThrows(IOException.class, () -> ImageService.valider(dossier.resolve("nexiste-pas.png")));

        assertTrue(e.getMessage().contains("introuvable"));
    }

    @Test
    void unDossierNommeCommeUneImageEstRefuse() throws IOException {
        Path unDossier = Files.createDirectories(dossier.resolve("faux.png"));

        assertThrows(IOException.class, () -> ImageService.valider(unDossier));
    }

    @Test
    void validerNullEstRefuse() {
        assertThrows(IOException.class, () -> ImageService.valider(null));
    }

    @Test
    void uneImageTropLourdeEstRefusee() throws IOException {
        Path source = creerFichier("enorme.png", new byte[(int) ImageService.TAILLE_MAX_OCTETS + 1]);

        IOException e = assertThrows(IOException.class, () -> ImageService.valider(source));

        assertTrue(e.getMessage().contains("lourde"));
    }

    @Test
    void uneImagePileALaTailleMaxEstAcceptee() throws IOException {
        Path source = creerFichier("limite.png", new byte[(int) ImageService.TAILLE_MAX_OCTETS]);

        assertDoesNotThrow(() -> ImageService.valider(source));
    }

    @Test
    void validerNeCopieRien() throws IOException {
        ImageService.valider(creerFichier("photo.png", new byte[]{1}));

        assertTrue(dossierImagesVide());
    }

    // ---------- resoudre / adresse ----------

    @Test
    void resoudreRenvoieNullPourUnNomVideOuNull() {
        assertNull(ImageService.resoudre(null));
        assertNull(ImageService.resoudre(""));
        assertNull(ImageService.resoudre("   "));
    }

    @Test
    void resoudreRenvoieNullPourUnFichierQuiNExistePlus() {
        assertNull(ImageService.resoudre("disparu.png"));
    }

    @Test
    void resoudreRefuseDeSortirDuDossierImages() throws IOException {
        Files.write(dossier.resolve("secret.png"), new byte[]{1});

        assertNull(ImageService.resoudre("../secret.png"));
        assertNull(ImageService.resoudre(".."));
        assertNull(ImageService.resoudre("."));
    }

    @Test
    void adresseDonneUneAdresseFichierPourUneImageExistante() throws IOException {
        String nom = ImageService.importer(creerFichier("photo.png", new byte[]{1}));

        String adresse = ImageService.adresse(nom);

        assertNotNull(adresse);
        assertTrue(adresse.startsWith("file:"));
        assertTrue(adresse.endsWith(nom));
    }

    @Test
    void adresseRenvoieNullQuandLaPhotoABesoinDEtreRemplacee() {
        assertNull(ImageService.adresse(null));
        assertNull(ImageService.adresse("disparue.png"));
    }

    // ---------- supprimer ----------

    @Test
    void supprimerEffaceLaCopie() throws IOException {
        String nom = ImageService.importer(creerFichier("photo.png", new byte[]{1}));

        assertTrue(ImageService.supprimer(nom));

        assertNull(ImageService.resoudre(nom));
    }

    @Test
    void supprimerNeToucheJamaisAuFichierDOrigine() throws IOException {
        Path source = creerFichier("photo.png", new byte[]{1});
        String nom = ImageService.importer(source);

        ImageService.supprimer(nom);

        assertTrue(Files.exists(source));
    }

    @Test
    void supprimerUneCopieDejaAbsenteRenvoieFalseSansErreur() {
        assertFalse(ImageService.supprimer("absent.png"));
        assertFalse(ImageService.supprimer(null));
        assertFalse(ImageService.supprimer(""));
    }

    @Test
    void supprimerRefuseDeSortirDuDossierImages() throws IOException {
        Path voisin = dossier.resolve("important.png");
        Files.write(voisin, new byte[]{1});

        assertFalse(ImageService.supprimer("../important.png"));

        assertTrue(Files.exists(voisin));
    }
}
