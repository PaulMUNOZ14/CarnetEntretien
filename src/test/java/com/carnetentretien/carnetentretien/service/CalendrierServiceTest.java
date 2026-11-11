package com.carnetentretien.carnetentretien.service;

import com.carnetentretien.carnetentretien.model.ControleTechnique;
import com.carnetentretien.carnetentretien.model.Entretien;
import com.carnetentretien.carnetentretien.model.EntretienPlanifie;
import com.carnetentretien.carnetentretien.model.Preconisation;
import com.carnetentretien.carnetentretien.model.Vehicule;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CalendrierServiceTest {

    private static final LocalDate AUJOURDHUI = LocalDate.now();

    private static Vehicule voiture(int id) {
        return new Vehicule(id, "AB-123-CD", "Peugeot", "208", 50000, 10000);
    }

    private static Preconisation regle(String operation, int intervalleKm, int intervalleMois) {
        return new Preconisation(0, operation, intervalleKm, intervalleMois);
    }

    private static Entretien fait(String operation, LocalDate date, int km) {
        return new Entretien(0, 1, operation, date, km, 50);
    }

    private static EntretienPlanifie planifie(String operation, LocalDate date) {
        return new EntretienPlanifie(0, 1, operation, date, null);
    }

    private static List<EvenementCalendrier> assembler(Vehicule v, List<Entretien> historique,
                                                       List<EntretienPlanifie> planifies, Preconisation... regles) {
        return CalendrierService.assembler(
                List.of(v),
                List.of(regles),
                Map.of(v.getId(), historique),
                Map.of(v.getId(), planifies));
    }

    @Test
    void lHistoriqueEtLEcheanceSontRassembles() {
        Vehicule v = voiture(1);
        List<EvenementCalendrier> evenements = assembler(v,
                List.of(fait("Vidange", AUJOURDHUI.minusMonths(1), 40000)),
                List.of(),
                regle("Vidange", 20000, 120));

        assertEquals(2, evenements.size());
        assertEquals(EvenementCalendrier.Type.REALISE, evenements.get(0).getType());
        assertEquals(EvenementCalendrier.Type.ECHEANCE, evenements.get(1).getType());
        assertEquals(AUJOURDHUI.plusDays(365), evenements.get(1).getDate());
        assertEquals("Vidange", evenements.get(1).getOperation());
        assertFalse(evenements.get(1).isEnRetard());
    }

    @Test
    void uneEcheanceDepasseeEstSignalee() {
        Vehicule v = voiture(1);
        List<EvenementCalendrier> evenements = assembler(v,
                List.of(fait("Vidange", AUJOURDHUI.minusMonths(1), 40000)),
                List.of(),
                regle("Vidange", 20000, 120));
        // Le véhicule a 50 000 km : on force le dépassement en le faisant rouler davantage.
        Vehicule depasse = new Vehicule(1, "AB-123-CD", "Peugeot", "208", 70000, 10000);
        evenements = assembler(depasse,
                List.of(fait("Vidange", AUJOURDHUI.minusMonths(1), 40000)),
                List.of(),
                regle("Vidange", 20000, 120));

        EvenementCalendrier echeance = evenements.stream()
                .filter(e -> e.getType() == EvenementCalendrier.Type.ECHEANCE)
                .findFirst().orElseThrow();
        assertTrue(echeance.isEnRetard());
        assertEquals(150, echeance.getPourcentage(), 0.0001);
    }

    @Test
    void unePlanificationDuMemeEntretienRemplaceLEcheanceCalculee() {
        Vehicule v = voiture(1);
        List<EvenementCalendrier> evenements = assembler(v,
                List.of(fait("Vidange", AUJOURDHUI.minusMonths(1), 40000)),
                List.of(planifie("Vidange", AUJOURDHUI.plusDays(10))),
                regle("Vidange", 20000, 120));

        assertEquals(2, evenements.size());
        assertEquals(EvenementCalendrier.Type.REALISE, evenements.get(0).getType());
        assertEquals(EvenementCalendrier.Type.PLANIFIE, evenements.get(1).getType());
        assertTrue(evenements.stream().noneMatch(e -> e.getType() == EvenementCalendrier.Type.ECHEANCE));
    }

    @Test
    void laComparaisonDesOperationsIgnoreLaCasseEtLesEspaces() {
        Vehicule v = voiture(1);
        List<EvenementCalendrier> evenements = assembler(v,
                List.of(fait("Vidange", AUJOURDHUI.minusMonths(1), 40000)),
                List.of(planifie("  vidange ", AUJOURDHUI.plusDays(10))),
                regle("VIDANGE", 20000, 120));

        assertTrue(evenements.stream().noneMatch(e -> e.getType() == EvenementCalendrier.Type.ECHEANCE));
    }

    @Test
    void unePlanificationDUneAutreOperationNeMasquePasLEcheance() {
        Vehicule v = voiture(1);
        List<EvenementCalendrier> evenements = assembler(v,
                List.of(fait("Vidange", AUJOURDHUI.minusMonths(1), 40000)),
                List.of(planifie("Pneus avant", AUJOURDHUI.plusDays(10))),
                regle("Vidange", 20000, 120));

        assertEquals(3, evenements.size());
        assertTrue(evenements.stream().anyMatch(e -> e.getType() == EvenementCalendrier.Type.ECHEANCE));
    }

    @Test
    void sansHistoriqueAucuneEcheanceNEstProduite() {
        Vehicule v = voiture(1);
        List<EvenementCalendrier> evenements = assembler(v, List.of(), List.of(), regle("Vidange", 20000, 120));

        assertTrue(evenements.isEmpty());
    }

    @Test
    void lesEvenementsSontTriesParDatePuisParType() {
        Vehicule v = voiture(1);
        LocalDate ancien = AUJOURDHUI.minusMonths(3);
        LocalDate recent = AUJOURDHUI.minusMonths(1);
        List<EvenementCalendrier> evenements = assembler(v,
                List.of(fait("Freins", recent, 48000), fait("Vidange", ancien, 30000)),
                List.of(planifie("Pneus", ancien.plusDays(1))),
                regle("Vidange", 20000, 120));

        assertEquals(4, evenements.size());
        assertEquals(ancien, evenements.get(0).getDate());
        assertEquals(EvenementCalendrier.Type.REALISE, evenements.get(0).getType());
        assertEquals(EvenementCalendrier.Type.PLANIFIE, evenements.get(1).getType());
        assertEquals(recent, evenements.get(2).getDate());
        for (int i = 1; i < evenements.size(); i++) {
            assertFalse(evenements.get(i).getDate().isBefore(evenements.get(i - 1).getDate()));
        }
    }

    @Test
    void lesEvenementsDePlusieursVehiculesSontRassembles() {
        Vehicule a = voiture(1);
        Vehicule b = new Vehicule(2, null, "Renault", "Clio", 30000, 8000);
        List<EvenementCalendrier> evenements = CalendrierService.assembler(
                List.of(a, b),
                List.of(),
                Map.of(a.getId(), List.of(fait("Vidange", AUJOURDHUI.minusDays(1), 49000)),
                        b.getId(), List.of(fait("Freins", AUJOURDHUI.minusDays(2), 29000))),
                Map.of(a.getId(), List.of(), b.getId(), List.of()));

        assertEquals(2, evenements.size());
        assertEquals("Freins", evenements.get(0).getOperation());
        assertEquals("Vidange", evenements.get(1).getOperation());
    }

    // ---------- Contrôle technique ----------

    /** Assemble les événements d'un seul véhicule, avec ses contrôles techniques et planifications. */
    private static List<EvenementCalendrier> assemblerAvecControles(Vehicule v,
                                                                   List<ControleTechnique> controles,
                                                                   List<EntretienPlanifie> planifies) {
        return CalendrierService.assembler(
                List.of(v),
                List.of(),
                Map.of(v.getId(), List.<Entretien>of()),
                Map.of(v.getId(), planifies),
                Map.of(v.getId(), controles));
    }

    @Test
    void unControleTechniqueDonneUneEcheanceEtUneLigneDHistorique() {
        Vehicule v = voiture(1);
        LocalDate realisation = AUJOURDHUI.minusYears(1);
        ControleTechnique controle = new ControleTechnique(0, v.getId(), realisation,
                ControleTechnique.Resultat.FAVORABLE, realisation.plusYears(2), null);

        List<EvenementCalendrier> evenements = assemblerAvecControles(v, List.of(controle), List.of());

        assertEquals(2, evenements.size());
        assertEquals(EvenementCalendrier.Type.CONTROLE_FAIT, evenements.get(0).getType());
        assertEquals(realisation, evenements.get(0).getDate());
        assertEquals(EvenementCalendrier.Type.CONTROLE, evenements.get(1).getType());
        assertEquals(realisation.plusYears(2), evenements.get(1).getDate());
        assertEquals(ControleTechniqueService.OPERATION, evenements.get(1).getOperation());
        assertFalse(evenements.get(1).isProche());
        assertFalse(evenements.get(1).isEnRetard());
    }

    @Test
    void uneContreVisiteEstProcheMaisPasEnRetard() {
        Vehicule v = voiture(1);
        LocalDate realisation = AUJOURDHUI.minusMonths(1);
        ControleTechnique controle = new ControleTechnique(0, v.getId(), realisation,
                ControleTechnique.Resultat.DEFAVORABLE, realisation.plusMonths(2), null);

        List<EvenementCalendrier> evenements = assemblerAvecControles(v, List.of(controle), List.of());

        EvenementCalendrier echeance = evenements.stream()
                .filter(e -> e.getType() == EvenementCalendrier.Type.CONTROLE)
                .findFirst().orElseThrow();
        assertTrue(echeance.isProche());
        assertFalse(echeance.isEnRetard());
    }

    @Test
    void uneEcheanceDepasseeDuControleTechniqueEstEnRetard() {
        Vehicule v = voiture(1);
        LocalDate prochaine = AUJOURDHUI.minusDays(10);
        ControleTechnique controle = new ControleTechnique(0, v.getId(), prochaine.minusYears(3),
                ControleTechnique.Resultat.FAVORABLE, prochaine, null);

        List<EvenementCalendrier> evenements = assemblerAvecControles(v, List.of(controle), List.of());

        EvenementCalendrier echeance = evenements.stream()
                .filter(e -> e.getType() == EvenementCalendrier.Type.CONTROLE)
                .findFirst().orElseThrow();
        assertEquals(prochaine, echeance.getDate());
        assertTrue(echeance.isEnRetard());
        assertTrue(echeance.isProche());
    }

    @Test
    void seulLeControleLePlusRecentGenereUneEcheance() {
        Vehicule v = voiture(1);
        ControleTechnique ancien = new ControleTechnique(0, v.getId(), AUJOURDHUI.minusYears(3),
                ControleTechnique.Resultat.FAVORABLE, AUJOURDHUI.minusYears(1), null);
        ControleTechnique recent = new ControleTechnique(0, v.getId(), AUJOURDHUI.minusYears(1),
                ControleTechnique.Resultat.FAVORABLE, AUJOURDHUI.plusYears(1), null);

        List<EvenementCalendrier> evenements = assemblerAvecControles(v, List.of(ancien, recent), List.of());

        assertEquals(3, evenements.size());
        long echeances = evenements.stream()
                .filter(e -> e.getType() == EvenementCalendrier.Type.CONTROLE)
                .count();
        assertEquals(1, echeances);
        assertEquals(AUJOURDHUI.plusYears(1), evenements.get(evenements.size() - 1).getDate());
    }

    @Test
    void leControleTechniqueNestPasMasqueParUnePlanificationDuMemeNom() {
        Vehicule v = voiture(1);
        ControleTechnique controle = new ControleTechnique(0, v.getId(), AUJOURDHUI.minusMonths(1),
                ControleTechnique.Resultat.FAVORABLE, AUJOURDHUI.plusMonths(23), null);
        EntretienPlanifie planifie = new EntretienPlanifie(0, v.getId(),
                ControleTechniqueService.OPERATION, AUJOURDHUI.plusDays(5), null);

        List<EvenementCalendrier> evenements = assemblerAvecControles(v, List.of(controle), List.of(planifie));

        assertEquals(3, evenements.size());
        assertEquals(EvenementCalendrier.Type.CONTROLE_FAIT, evenements.get(0).getType());
        assertEquals(EvenementCalendrier.Type.PLANIFIE, evenements.get(1).getType());
        assertEquals(EvenementCalendrier.Type.CONTROLE, evenements.get(2).getType());
        assertTrue(evenements.stream().noneMatch(e -> e.getType() == EvenementCalendrier.Type.ECHEANCE));
    }
}
