package com.carnetentretien.carnetentretien.service;

import com.carnetentretien.carnetentretien.model.Entretien;
import com.carnetentretien.carnetentretien.model.Preconisation;
import com.carnetentretien.carnetentretien.model.Vehicule;
import com.carnetentretien.carnetentretien.service.CalculateurEntretien.Bilan;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Les dates sont calculées à partir d'aujourd'hui : les tests donnent le même résultat quel que soit le jour. */
class CalculateurEntretienTest {

    private static final LocalDate AUJOURDHUI = LocalDate.now();

    private static Vehicule voiture(int km, int kmAnnuel) {
        return new Vehicule(1, null, "Peugeot", "208", km, kmAnnuel);
    }

    private static Entretien fait(String operation, LocalDate date, int km) {
        return new Entretien(0, 1, operation, date, km, 50);
    }

    private static Preconisation regle(String operation, int intervalleKm, int intervalleMois) {
        return new Preconisation(0, operation, intervalleKm, intervalleMois);
    }

    private static Bilan unSeulBilan(Vehicule v, List<Entretien> historique, Preconisation p) {
        List<Bilan> bilans = CalculateurEntretien.analyserEntretien(v, historique, List.of(p));
        assertEquals(1, bilans.size());
        return bilans.get(0);
    }

    @Test
    void sansAucuneRegleIlNYAAucunBilan() {
        assertTrue(CalculateurEntretien.analyserEntretien(voiture(50000, 12000), List.of(), List.of()).isEmpty());
    }

    @Test
    void sansHistoriqueOnDemandeDeRenseignerLaDerniereIntervention() {
        Bilan b = unSeulBilan(voiture(50000, 12000), List.of(), regle("Vidange", 15000, 12));

        assertEquals("Vidange", b.piece);
        assertEquals(0, b.pourcentage, 0.0001);
        assertTrue(b.statut.contains("Action Requise"));
        assertTrue(b.details.contains("Aucun historique"));
        assertNull(b.dateEcheance);
        assertFalse(b.depasse);
    }

    @Test
    void unEntretienRecentEstOkEtLePourcentageSuitLesKilometres() {
        // 10 000 km parcourus sur 20 000 autorisés = 50 %
        Bilan b = unSeulBilan(voiture(50000, 10000),
                List.of(fait("Vidange", AUJOURDHUI.minusMonths(1), 40000)),
                regle("Vidange", 20000, 120));

        assertEquals(50, b.pourcentage, 0.0001);
        assertTrue(b.statut.contains("OK"));
    }

    @Test
    void laDateDEcheanceEstEstimeeAPartirDuKilometrageAnnuel() {
        // Il reste 10 000 km à 10 000 km/an : échéance dans 365 jours
        Bilan b = unSeulBilan(voiture(50000, 10000),
                List.of(fait("Vidange", AUJOURDHUI.minusMonths(1), 40000)),
                regle("Vidange", 20000, 120));

        assertTrue(b.details.contains("À planifier pour le : " + AUJOURDHUI.plusDays(365)));
        assertTrue(b.details.contains("Atteinte des 60000 km"));
        assertTrue(b.details.contains("Dernier entretien le " + AUJOURDHUI.minusMonths(1) + " à 40000 km"));
        assertEquals(AUJOURDHUI.plusDays(365), b.dateEcheance);
        assertFalse(b.depasse);
    }

    @Test
    void entreQuatreVingtsEtCentPourCentOnDitDePrevoir() {
        // 17 000 km sur 20 000 = 85 %
        Bilan b = unSeulBilan(voiture(57000, 10000),
                List.of(fait("Vidange", AUJOURDHUI.minusMonths(1), 40000)),
                regle("Vidange", 20000, 120));

        assertEquals(85, b.pourcentage, 0.0001);
        assertTrue(b.statut.contains("À PRÉVOIR"));
    }

    @Test
    void auDelaDeLIntervalleKilometriqueCEstUrgent() {
        // 30 000 km sur 20 000 = 150 %
        Bilan b = unSeulBilan(voiture(70000, 10000),
                List.of(fait("Vidange", AUJOURDHUI.minusMonths(1), 40000)),
                regle("Vidange", 20000, 120));

        assertEquals(150, b.pourcentage, 0.0001);
        assertTrue(b.statut.contains("URGENT"));
        assertTrue(b.details.contains("Dépassé depuis le " + AUJOURDHUI));
        assertTrue(b.details.contains("Atteinte des 60000 km"));
        assertEquals(AUJOURDHUI, b.dateEcheance);
        assertTrue(b.depasse);
    }

    @Test
    void leDelaiEnMoisPeutDeclencherLUrgenceMemeAvecPeuDeKilometres() {
        // 24 mois écoulés pour une règle à 12 mois = 200 %, alors que 1 000 km sur 15 000 = 7 %
        LocalDate dernier = AUJOURDHUI.minusMonths(24);
        Bilan b = unSeulBilan(voiture(41000, 12000), List.of(fait("Vidange", dernier, 40000)), regle("Vidange", 15000, 12));

        assertEquals(200, b.pourcentage, 0.0001);
        assertTrue(b.statut.contains("URGENT"));
        assertTrue(b.details.contains("Délai de 12 mois écoulé"));
        assertTrue(b.details.contains("Dépassé depuis le " + dernier.plusMonths(12)));
        assertEquals(dernier.plusMonths(12), b.dateEcheance);
        assertTrue(b.depasse);
    }

    @Test
    void lePlusGrandDesDeuxPourcentagesEstRetenu() {
        // km : 50 %, temps : 6 mois sur 12 = 50 %. On passe à 9 mois : 75 % > 50 %
        Bilan b = unSeulBilan(voiture(50000, 10000),
                List.of(fait("Vidange", AUJOURDHUI.minusMonths(9), 40000)),
                regle("Vidange", 20000, 12));

        assertEquals(75, b.pourcentage, 0.0001);
    }

    @Test
    void leNomDeLOperationEstCompareSansTenirCompteDeLaCasse() {
        Bilan b = unSeulBilan(voiture(50000, 10000),
                List.of(fait("vidange", AUJOURDHUI.minusMonths(1), 40000)),
                regle("VIDANGE", 20000, 120));

        assertEquals(50, b.pourcentage, 0.0001);
    }

    @Test
    void lesEntretiensDUneAutreOperationNeComptentPas() {
        Bilan b = unSeulBilan(voiture(50000, 10000),
                List.of(fait("Pneus", AUJOURDHUI.minusMonths(1), 49000)),
                regle("Vidange", 20000, 120));

        assertTrue(b.statut.contains("Action Requise"));
    }

    @Test
    void lEntretienLePlusRecentParDateEstCeluiQuiEstUtilise() {
        List<Entretien> historique = List.of(
                fait("Vidange", AUJOURDHUI.minusMonths(1), 40000),    // le plus récent
                fait("Vidange", AUJOURDHUI.minusMonths(30), 10000));  // ancien

        Bilan b = unSeulBilan(voiture(50000, 10000), historique, regle("Vidange", 20000, 120));

        assertEquals(50, b.pourcentage, 0.0001);
        assertTrue(b.details.contains("à 40000 km"));
    }

    @Test
    void unKilometrageAnnuelNulOuNegatifUtiliseLaValeurParDefautDeDixMilleKilometres() {
        List<Entretien> historique = List.of(fait("Vidange", AUJOURDHUI.minusMonths(1), 40000));
        Preconisation vidange = regle("Vidange", 20000, 120);

        Bilan nul = unSeulBilan(voiture(50000, 0), historique, vidange);
        Bilan negatif = unSeulBilan(voiture(50000, -5), historique, vidange);
        Bilan reference = unSeulBilan(voiture(50000, 10000), historique, vidange);

        assertEquals(reference.details, nul.details);
        assertEquals(reference.details, negatif.details);
    }

    @Test
    void unBilanEstProduitParRegleDansLOrdreDesRegles() {
        List<Bilan> bilans = CalculateurEntretien.analyserEntretien(
                voiture(50000, 10000),
                List.of(fait("Vidange", AUJOURDHUI.minusMonths(1), 49000)),
                List.of(regle("Pneus", 40000, 48), regle("Vidange", 15000, 12), regle("Freins", 40000, 48)));

        assertEquals(3, bilans.size());
        assertEquals("Pneus", bilans.get(0).piece);
        assertEquals("Vidange", bilans.get(1).piece);
        assertEquals("Freins", bilans.get(2).piece);
    }
}
