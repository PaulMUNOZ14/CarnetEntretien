package com.carnetentretien.carnetentretien.service;

import com.carnetentretien.carnetentretien.BaseDonneesTest;
import com.carnetentretien.carnetentretien.service.RecherchePlaqueService.Resultat;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Aucun de ces tests n'appelle le vrai service : la réponse HTTP est fournie à la main. */
class RecherchePlaqueServiceTest extends BaseDonneesTest {

    private static final String REPONSE_OK = """
            {"api-version":"1.0","message":"OK","code_erreur":200,
             "data":{"erreur":"","immat":"AB123CD","pays":"FR","marque":"RENAULT","modele":"MEGANE III COUPE",
                     "version":"1.5 DCI 110","energieNGC":"Diesel","date1erCir_fr":"12/03/2012",
                     "photo_modele":"//cdn.example.com/photo.jpg","logo_marque":"https://cdn.example.com/logo.png"}}
            """;

    // ---------- plaque : normaliser / valider / formater ----------

    @Test
    void normaliserMetEnMajusculesEtRetireLesSeparateurs() {
        assertEquals("AB123CD", RecherchePlaqueService.normaliser(" ab-123 cd "));
        assertEquals("", RecherchePlaqueService.normaliser(null));
    }

    @Test
    void formatValideAccepteLesPlaquesSIV() {
        assertTrue(RecherchePlaqueService.formatValide("AB-123-CD"));
        assertTrue(RecherchePlaqueService.formatValide("ab 123 cd"));
        assertTrue(RecherchePlaqueService.formatValide("AB123CD"));
    }

    @Test
    void formatValideAccepteLesAnciennesPlaquesFNI() {
        assertTrue(RecherchePlaqueService.formatValide("1234 AB 56"));
        assertTrue(RecherchePlaqueService.formatValide("123 ABC 75"));
    }

    @Test
    void formatValideRefuseLesPlaquesIncorrectes() {
        assertFalse(RecherchePlaqueService.formatValide("AB-12-CD"));
        assertFalse(RecherchePlaqueService.formatValide("ABC-123-DE"));
        assertFalse(RecherchePlaqueService.formatValide("12345"));
        assertFalse(RecherchePlaqueService.formatValide(""));
        assertFalse(RecherchePlaqueService.formatValide("   "));
        assertFalse(RecherchePlaqueService.formatValide(null));
    }

    @Test
    void formaterMetLesTiretsDesPlaquesSIV() {
        assertEquals("AB-123-CD", RecherchePlaqueService.formater("ab123cd"));
        assertEquals("AB-123-CD", RecherchePlaqueService.formater(" ab 123 cd "));
    }

    @Test
    void formaterLaisseUneAnciennePlaqueTelleQuelleEnMajuscules() {
        assertEquals("1234 AB 56", RecherchePlaqueService.formater(" 1234 ab 56 "));
        assertEquals("", RecherchePlaqueService.formater(null));
    }

    @Test
    void rechercherRefuseUnePlaqueInvalideSansAppelerLeReseau() {
        Resultat r = RecherchePlaqueService.rechercher("pas une plaque");

        assertFalse(r.trouve());
        assertTrue(r.messageErreur().contains("plaque française"));
    }

    // ---------- interpreter : réponse du service ----------

    @Test
    void uneReponseCompleteDonneLeVehiculeMisEnForme() {
        Resultat r = RecherchePlaqueService.interpreter(200, REPONSE_OK, false);

        assertTrue(r.trouve());
        assertEquals("Renault", r.marque());
        assertEquals("Megane III Coupe", r.modele());   // chiffres romains conservés en majuscules
        assertEquals("1.5 DCI 110", r.version());
        assertEquals("2012", r.annee());
        assertEquals("Diesel", r.energie());
        assertNull(r.messageErreur());
    }

    @Test
    void lesAdressesDImageSontCompleteesEnHttps() {
        Resultat r = RecherchePlaqueService.interpreter(200, REPONSE_OK, false);

        assertEquals("https://cdn.example.com/photo.jpg", r.imageUrl());   // « //… » devient « https://… »
        assertEquals("https://cdn.example.com/logo.png", r.logoUrl());
    }

    @Test
    void uneAdresseDImageNonHttpEstIgnoree() {
        String json = REPONSE_OK.replace("//cdn.example.com/photo.jpg", "javascript:alert(1)")
                .replace("https://cdn.example.com/logo.png", "ftp://x/logo.png");

        Resultat r = RecherchePlaqueService.interpreter(200, json, false);

        assertTrue(r.trouve());
        assertNull(r.imageUrl());
        assertNull(r.logoUrl());
    }

    @Test
    void sansPhotoLeVehiculeEstQuandMemeTrouve() {
        String json = REPONSE_OK.replace("\"photo_modele\":\"//cdn.example.com/photo.jpg\",", "");

        Resultat r = RecherchePlaqueService.interpreter(200, json, false);

        assertTrue(r.trouve());
        assertNull(r.imageUrl());
    }

    @Test
    void lesSiglesEtLesTiretsDesMarquesSontRespectes() {
        assertEquals("BMW", RecherchePlaqueService.interpreter(200, avecMarque("BMW", "SERIE 1"), false).marque());
        assertEquals("Rolls-Royce", RecherchePlaqueService.interpreter(200, avecMarque("ROLLS-ROYCE", "GHOST"), false).marque());
        assertEquals("Volkswagen", RecherchePlaqueService.interpreter(200, avecMarque("VOLKSWAGEN", "GOLF"), false).marque());
    }

    @Test
    void unModeleAvecDesChiffresGardeSesMajuscules() {
        assertEquals("208 GTI", RecherchePlaqueService.interpreter(200, avecMarque("PEUGEOT", "208 GTI"), false).modele());
    }

    @Test
    void lesCaracteresEchappesDuJsonSontDecodes() {
        Resultat r = RecherchePlaqueService.interpreter(200, avecMarque("CITRO\\u00cbN", "C3"), false);

        assertEquals("Citroën", r.marque());
    }

    @Test
    void uneEnergieNumeriqueEstIgnoree() {
        String json = REPONSE_OK.replace("\"energieNGC\":\"Diesel\"", "\"energieNGC\":\"3\"");

        assertEquals("", RecherchePlaqueService.interpreter(200, json, false).energie());
    }

    @Test
    void uneDateIllisibleDonneUneAnneeNull() {
        String json = REPONSE_OK.replace("12/03/2012", "inconnue");

        assertNull(RecherchePlaqueService.interpreter(200, json, false).annee());
    }

    // ---------- interpreter : erreurs ----------

    @Test
    void uneCleRefuseeEnvoieVersLeBoutonCleApi() {
        for (int statut : new int[]{401, 403}) {
            Resultat r = RecherchePlaqueService.interpreter(statut, "", false);

            assertFalse(r.trouve());
            assertTrue(r.messageErreur().contains("clé"));
            assertTrue(r.messageErreur().contains("Clé API"));
        }
    }

    @Test
    void tropDeRecherchesDonneUnMessageDAttente() {
        Resultat r = RecherchePlaqueService.interpreter(429, "", false);

        assertFalse(r.trouve());
        assertTrue(r.messageErreur().contains("Trop de recherches"));
    }

    @Test
    void unAutreCodeHttpEstAfficheDansLeMessage() {
        Resultat r = RecherchePlaqueService.interpreter(500, "", false);

        assertFalse(r.trouve());
        assertTrue(r.messageErreur().contains("500"));
    }

    @Test
    void uneReponseQuiNEstPasDuJsonEstSignaleeSansPlanter() {
        Resultat r = RecherchePlaqueService.interpreter(200, "<html>Maintenance</html>", false);

        assertFalse(r.trouve());
        assertTrue(r.messageErreur().contains("inattendue"));
    }

    @Test
    void unJsonTronqueEstSignaleSansPlanter() {
        Resultat r = RecherchePlaqueService.interpreter(200, "{\"data\":{\"marque\":\"REN", false);

        assertFalse(r.trouve());
    }

    @Test
    void unCorpsNullEstSignaleSansPlanter() {
        assertFalse(RecherchePlaqueService.interpreter(200, null, false).trouve());
    }

    @Test
    void unJsonQuiNEstPasUnObjetEstIllisible() {
        Resultat r = RecherchePlaqueService.interpreter(200, "[1,2,3]", false);

        assertFalse(r.trouve());
        assertTrue(r.messageErreur().contains("illisible"));
    }

    @Test
    void sansBlocDataLaPlaqueEstIntrouvableEtLeDetailDuServiceEstRepris() {
        Resultat r = RecherchePlaqueService.interpreter(200, "{\"code_erreur\":404,\"message\":\"Plaque inconnue\"}", false);

        assertFalse(r.trouve());
        assertTrue(r.messageErreur().contains("Aucun véhicule trouvé"));
        assertTrue(r.messageErreur().contains("Plaque inconnue"));
    }

    @Test
    void uneErreurDansLeBlocDataEstReprise() {
        String json = REPONSE_OK.replace("\"erreur\":\"\"", "\"erreur\":\"Immatriculation non trouvée\"");

        Resultat r = RecherchePlaqueService.interpreter(200, json, false);

        assertFalse(r.trouve());
        assertTrue(r.messageErreur().contains("Immatriculation non trouvée"));
    }

    @Test
    void unCodeErreurDifferentDe200EstUnEchecMemeAvecDesDonnees() {
        String json = REPONSE_OK.replace("\"code_erreur\":200", "\"code_erreur\":500");

        assertFalse(RecherchePlaqueService.interpreter(200, json, false).trouve());
    }

    @Test
    void sansMarqueOuSansModeleLaRechercheEstUnEchec() {
        assertFalse(RecherchePlaqueService.interpreter(200, REPONSE_OK.replace("RENAULT", ""), false).trouve());
        assertFalse(RecherchePlaqueService.interpreter(200, REPONSE_OK.replace("MEGANE III COUPE", ""), false).trouve());
    }

    @Test
    void avecLaCleDeDemonstrationLeMessageProposeDAjouterSaPropreCle() {
        String json = "{\"code_erreur\":404,\"data\":null}";

        assertTrue(RecherchePlaqueService.interpreter(200, json, true).messageErreur().contains("clé de démonstration"));
        assertFalse(RecherchePlaqueService.interpreter(200, json, false).messageErreur().contains("clé de démonstration"));
    }

    // ---------- clé utilisée ----------

    @Test
    void sansCleEnregistreeLaCleDeDemonstrationEstUtilisee() {
        Assumptions.assumeTrue(System.getenv("PLAQUE_API_TOKEN") == null);

        assertTrue(RecherchePlaqueService.utiliseCleDemo());
    }

    @Test
    void laCleEnregistreeParLUtilisateurEstCelleQuiEstUtilisee() {
        Assumptions.assumeTrue(System.getenv("PLAQUE_API_TOKEN") == null);

        ConfigService.setCleApi("ma-cle-perso");

        assertEquals("ma-cle-perso", RecherchePlaqueService.lireToken());
        assertFalse(RecherchePlaqueService.utiliseCleDemo());
    }

    @Test
    void supprimerLaCleRameneLaCleDeDemonstration() {
        Assumptions.assumeTrue(System.getenv("PLAQUE_API_TOKEN") == null);
        ConfigService.setCleApi("ma-cle-perso");

        ConfigService.setCleApi(null);

        assertTrue(RecherchePlaqueService.utiliseCleDemo());
    }

    private static String avecMarque(String marque, String modele) {
        return REPONSE_OK.replace("RENAULT", marque).replace("MEGANE III COUPE", modele);
    }
}
