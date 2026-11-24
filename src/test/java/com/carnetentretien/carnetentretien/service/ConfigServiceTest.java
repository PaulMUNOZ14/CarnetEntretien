package com.carnetentretien.carnetentretien.service;

import com.carnetentretien.carnetentretien.BaseDonneesTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ConfigServiceTest extends BaseDonneesTest {

    @Test
    void sansFichierIlNYAPasDeCle() {
        assertEquals(Optional.empty(), ConfigService.getCleApi());
        assertFalse(ConfigService.aCleApi());
    }

    @Test
    void uneCleEnregistreeEstRetrouvee() {
        ConfigService.setCleApi("abc123XYZ");

        assertEquals(Optional.of("abc123XYZ"), ConfigService.getCleApi());
        assertTrue(ConfigService.aCleApi());
    }

    @Test
    void lesEspacesEtSautsDeLigneAutourDeLaCleSontRetires() throws IOException {
        ConfigService.setCleApi("  abc123 \n");

        assertEquals(Optional.of("abc123"), ConfigService.getCleApi());
        // et le fichier lui-même contient la clé propre, pas une version avec espaces
        assertTrue(Files.readAllLines(AppPaths.fichierConfiguration()).contains("plaque.api.token=abc123"));
    }

    @Test
    void enregistrerUneCleVideOuNullLaSupprime() {
        ConfigService.setCleApi("abc123");
        ConfigService.setCleApi("   ");
        assertFalse(ConfigService.aCleApi());

        ConfigService.setCleApi("abc123");
        ConfigService.setCleApi(null);
        assertFalse(ConfigService.aCleApi());
    }

    @Test
    void uneNouvelleCleRemplaceLAncienne() {
        ConfigService.setCleApi("ancienne");
        ConfigService.setCleApi("nouvelle");

        assertEquals(Optional.of("nouvelle"), ConfigService.getCleApi());
    }

    @Test
    void laCleEstEcriteSousLaCleHistoriquePlaquePointApiPointToken() throws IOException {
        ConfigService.setCleApi("ma-cle");

        assertTrue(Files.readString(AppPaths.fichierConfiguration()).contains("plaque.api.token=ma-cle"));
    }

    @Test
    void unConfigPropertiesDejaPresentEstLuAvecSesCommentaires() throws IOException {
        Files.writeString(AppPaths.fichierConfiguration(), "plaque.api.token=ancienneCle123\n\n# un commentaire\n");

        assertEquals(Optional.of("ancienneCle123"), ConfigService.getCleApi());
    }

    @Test
    void enregistrerLaCleConserveLesAutresReglages() throws IOException {
        Files.writeString(AppPaths.fichierConfiguration(), "autre.reglage=valeur\n");

        ConfigService.setCleApi("abc123");

        String contenu = Files.readString(AppPaths.fichierConfiguration());
        assertTrue(contenu.contains("autre.reglage=valeur"));
        assertTrue(contenu.contains("plaque.api.token=abc123"));
    }

    @Test
    void lesCaracteresSpeciauxSontConservesTelsQuels() {
        String cle = "k=é:ü #!\\x/ç";

        ConfigService.setCleApi(cle);

        assertEquals(Optional.of(cle), ConfigService.getCleApi());
    }

    @Test
    void laCreationDuFichierCreeAussiLeDossierManquant() throws IOException {
        Files.delete(dossier);   // le dossier de données n'existe pas encore (premier lancement)

        ConfigService.setCleApi("abc123");

        assertEquals(Optional.of("abc123"), ConfigService.getCleApi());
    }

    @Test
    void laFenetreDAideEstMemoriseeSansToucherALaCle() {
        ConfigService.setCleApi("abc123");
        assertFalse(ConfigService.aideDejaAffichee());

        ConfigService.marquerAideAffichee();

        assertTrue(ConfigService.aideDejaAffichee());
        assertEquals(Optional.of("abc123"), ConfigService.getCleApi());

        ConfigService.setCleApi(null);
        assertTrue(ConfigService.aideDejaAffichee());
    }

    @Test
    void masquerCleNeLaisseVisibleQueLesQuatreDerniersCaracteres() {
        assertEquals("•••••••••6789", ConfigService.masquerCle("abcdefghi6789"));
    }

    @Test
    void masquerUneCleCourteLaCacheEntierement() {
        assertEquals("•••••", ConfigService.masquerCle("abcde"));
        assertEquals("••••••••", ConfigService.masquerCle("abcdefgh"));
    }

    @Test
    void masquerUneCleVideOuNullDonneUneChaineVide() {
        assertEquals("", ConfigService.masquerCle(null));
        assertEquals("", ConfigService.masquerCle("   "));
    }

    @Test
    void leLienPourObtenirUneCleVaVersLeServiceUtiliseParLApplication() {
        assertTrue(ConfigService.LIEN_OBTENIR_CLE.startsWith("https://apiplaqueimmatriculation.com/"));
    }

    // ---------- Notifications ----------

    @Test
    void lesNotificationsSontActivesParDefaut() {
        assertTrue(ConfigService.notificationsActives());
    }

    @Test
    void leChoixDesNotificationsEstMemoriseSansToucherALaCle() {
        ConfigService.setCleApi("abc123");

        ConfigService.setNotificationsActives(false);
        assertFalse(ConfigService.notificationsActives());
        assertEquals(Optional.of("abc123"), ConfigService.getCleApi());

        ConfigService.setNotificationsActives(true);
        assertTrue(ConfigService.notificationsActives());
        assertEquals(Optional.of("abc123"), ConfigService.getCleApi());
    }

    @Test
    void laDerniereNotificationEstMemoriseeParDate() {
        assertTrue(ConfigService.derniereNotification().isEmpty());

        LocalDate date = LocalDate.of(2026, 11, 24);
        ConfigService.setDerniereNotification(date);
        assertEquals(Optional.of(date), ConfigService.derniereNotification());

        ConfigService.setDerniereNotification(null);
        assertTrue(ConfigService.derniereNotification().isEmpty());
    }

    @Test
    void uneDateDeDerniereNotificationInvalideEstIgnoree() throws IOException {
        Files.writeString(AppPaths.fichierConfiguration(), "notifications.derniere=pas-une-date\n");

        assertTrue(ConfigService.derniereNotification().isEmpty());
    }

    @Test
    void lesReglagesDeNotificationSontEcritsDansLeFichier() throws IOException {
        ConfigService.setNotificationsActives(false);
        ConfigService.setDerniereNotification(LocalDate.of(2026, 11, 24));

        String contenu = Files.readString(AppPaths.fichierConfiguration());
        assertTrue(contenu.contains("notifications.actives=false"));
        assertTrue(contenu.contains("notifications.derniere=2026-11-24"));
    }
}
