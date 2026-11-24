package com.carnetentretien.carnetentretien.service;

import com.carnetentretien.carnetentretien.model.ControleTechnique;
import com.carnetentretien.carnetentretien.model.ControleTechniquePlanifie;
import com.carnetentretien.carnetentretien.model.Entretien;
import com.carnetentretien.carnetentretien.model.EntretienPlanifie;
import com.carnetentretien.carnetentretien.model.Vehicule;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RappelNotificationTest {

    private static final LocalDate AUJOURDHUI = LocalDate.now();
    private static final Vehicule PEUGEOT = new Vehicule(1, "AB-123-CD", "Peugeot", "208", 50000, 10000);
    private static final Vehicule CLIO = new Vehicule(2, null, "Renault", "Clio IV", 30000, 8000);

    private static EvenementCalendrier echeance(Vehicule vehicule, String operation,
                                                LocalDate date, double usure, boolean depasse) {
        CalculateurEntretien.Bilan bilan = new CalculateurEntretien.Bilan(
                operation, usure, "", "", date, depasse);
        return EvenementCalendrier.echeance(vehicule, bilan);
    }

    private static EvenementCalendrier controle(Vehicule vehicule, LocalDate prochaine, boolean proche) {
        ControleTechnique controle = new ControleTechnique(0, vehicule.getId(),
                prochaine.minusYears(2), ControleTechnique.Resultat.FAVORABLE, prochaine, null);
        return EvenementCalendrier.controle(vehicule, controle, proche);
    }

    private static EvenementCalendrier planifie(Vehicule vehicule, String operation, LocalDate date) {
        return EvenementCalendrier.planifie(vehicule,
                new EntretienPlanifie(0, vehicule.getId(), operation, date, null));
    }

    private static EvenementCalendrier controlePlanifie(Vehicule vehicule, LocalDate date) {
        return EvenementCalendrier.controlePlanifie(vehicule,
                new ControleTechniquePlanifie(0, vehicule.getId(), date));
    }

    private static EvenementCalendrier realise(Vehicule vehicule, LocalDate date) {
        return EvenementCalendrier.realise(vehicule,
                new Entretien(0, vehicule.getId(), "Vidange", date, 49000, 90));
    }

    private static EvenementCalendrier controleFait(Vehicule vehicule, LocalDate date) {
        return EvenementCalendrier.controleFait(vehicule,
                new ControleTechnique(0, vehicule.getId(), date,
                        ControleTechnique.Resultat.FAVORABLE, date.plusYears(2), null));
    }

    private static List<RappelNotification> collecter(EvenementCalendrier... evenements) {
        return RappelNotification.collecter(List.of(evenements), AUJOURDHUI);
    }

    // ---------- Sélection ----------

    @Test
    void uneEcheanceProcheEstRetenue() {
        List<RappelNotification> rappels = collecter(
                echeance(PEUGEOT, "Vidange", AUJOURDHUI.plusDays(10), 50, false));

        assertEquals(1, rappels.size());
        RappelNotification rappel = rappels.get(0);
        assertEquals("Vidange", rappel.getOperation());
        assertEquals("Peugeot 208 (AB-123-CD)", rappel.getVehicule());
        assertFalse(rappel.isEnRetard());
        assertTrue(rappel.getPrecision().startsWith("échéance le "));
    }

    @Test
    void uneEcheanceLointaineEtPeuUseNEstPasRetenue() {
        assertTrue(collecter(
                echeance(PEUGEOT, "Vidange", AUJOURDHUI.plusDays(60), 50, false)).isEmpty());
    }

    @Test
    void uneEcheanceLointaineMaisUseeAQuatreVingtsPourCentEstRetenue() {
        List<RappelNotification> rappels = collecter(
                echeance(PEUGEOT, "Pneus avant", AUJOURDHUI.plusDays(60), 85, false));

        assertEquals(1, rappels.size());
        assertEquals("Pneus avant", rappels.get(0).getOperation());
    }

    @Test
    void uneEcheanceDepasseeEstSignaleeEnRetard() {
        List<RappelNotification> rappels = collecter(
                echeance(PEUGEOT, "Vidange", AUJOURDHUI.minusDays(2), 120, true));

        assertEquals(1, rappels.size());
        assertTrue(rappels.get(0).isEnRetard());
    }

    @Test
    void unControleTechniqueProcheEstRetenu() {
        List<RappelNotification> rappels = collecter(
                controle(PEUGEOT, AUJOURDHUI.plusMonths(1), true));

        assertEquals(1, rappels.size());
        assertEquals(ControleTechniqueService.OPERATION, rappels.get(0).getOperation());
        assertFalse(rappels.get(0).isEnRetard());
    }

    @Test
    void unControleTechniqueLointainNEstPasRetenu() {
        assertTrue(collecter(controle(PEUGEOT, AUJOURDHUI.plusMonths(6), false)).isEmpty());
    }

    @Test
    void unePlanificationDansLaSemaineEstRetenue() {
        List<RappelNotification> rappels = collecter(
                planifie(PEUGEOT, "Vidange", AUJOURDHUI.plusDays(3)));

        assertEquals(1, rappels.size());
        assertTrue(rappels.get(0).getPrecision().startsWith("prévu le "));
    }

    @Test
    void unePlanificationLointaineNEstPasRetenue() {
        assertTrue(collecter(planifie(PEUGEOT, "Vidange", AUJOURDHUI.plusDays(15))).isEmpty());
    }

    @Test
    void unControleTechniquePlanifieProcheEstRetenu() {
        List<RappelNotification> rappels = collecter(
                controlePlanifie(PEUGEOT, AUJOURDHUI.plusDays(2)));

        assertEquals(1, rappels.size());
        assertTrue(rappels.get(0).getPrecision().startsWith("contrôle prévu le "));
    }

    @Test
    void unePlanificationPasseeResteSignalee() {
        List<RappelNotification> rappels = collecter(
                planifie(PEUGEOT, "Vidange", AUJOURDHUI.minusDays(3)));

        assertEquals(1, rappels.size());
        assertTrue(rappels.get(0).isEnRetard());
    }

    @Test
    void lesOperationsDejaRealiseesSontIgnorees() {
        assertTrue(collecter(
                realise(PEUGEOT, AUJOURDHUI.minusDays(2)),
                controleFait(PEUGEOT, AUJOURDHUI.minusMonths(3))).isEmpty());
    }

    @Test
    void lesRetardsPassentDevantLesEcheancesACourtTerme() {
        List<RappelNotification> rappels = collecter(
                echeance(PEUGEOT, "Vidange", AUJOURDHUI.plusDays(10), 50, false),
                echeance(CLIO, "Freins", AUJOURDHUI.minusDays(1), 110, true),
                planifie(PEUGEOT, "Pneus", AUJOURDHUI.plusDays(2)));

        assertEquals(3, rappels.size());
        assertTrue(rappels.get(0).isEnRetard());
        assertFalse(rappels.get(1).isEnRetard());
        assertFalse(rappels.get(2).isEnRetard());
        assertTrue(rappels.get(1).getDate().isBefore(rappels.get(2).getDate()));
    }

    // ---------- Textes ----------

    @Test
    void leTitreDistingueLesRetardsEtLesRappelsAPrevoir() {
        List<RappelNotification> rappels = collecter(
                echeance(CLIO, "Freins", AUJOURDHUI.minusDays(1), 110, true),
                echeance(PEUGEOT, "Vidange", AUJOURDHUI.plusDays(10), 50, false),
                planifie(PEUGEOT, "Pneus", AUJOURDHUI.plusDays(2)));

        assertEquals("1 rappel en retard, 2 à prévoir", RappelNotification.titre(rappels));
    }

    @Test
    void leTexteDetailleTroisRappelsEtCompteLesAutres() {
        List<RappelNotification> rappels = collecter(
                echeance(CLIO, "Freins", AUJOURDHUI.minusDays(1), 110, true),
                echeance(PEUGEOT, "Vidange", AUJOURDHUI.plusDays(10), 50, false),
                planifie(PEUGEOT, "Pneus", AUJOURDHUI.plusDays(2)),
                controle(PEUGEOT, AUJOURDHUI.plusMonths(1), true));

        String texte = RappelNotification.texte(rappels);
        assertTrue(texte.contains("• Freins — Renault Clio IV : échéance le "));
        assertTrue(texte.contains("• Vidange — Peugeot 208 (AB-123-CD) : échéance le "));
        assertTrue(texte.contains("• Pneus — Peugeot 208 (AB-123-CD) : prévu le "));
        assertTrue(texte.contains("et 1 autre rappel."));
        assertFalse(texte.contains("Contrôle technique"));
    }

    @Test
    void uneListeVideDonneUnTitreEtUnTexteNeutres() {
        assertEquals("Carnet Entretien", RappelNotification.titre(List.of()));
        assertTrue(RappelNotification.texte(List.of()).contains("tout est à jour"));
    }
}
