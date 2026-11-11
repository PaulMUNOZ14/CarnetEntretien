package com.carnetentretien.carnetentretien.service;

import com.carnetentretien.carnetentretien.model.ControleTechnique;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Les états sont calculés à partir d'aujourd'hui : les tests donnent le même résultat quel que soit le jour. */
class ControleTechniqueServiceTest {

    private static final LocalDate AUJOURDHUI = LocalDate.now();

    private static ControleTechnique controle(LocalDate realisation, ControleTechnique.Resultat resultat,
                                              LocalDate prochaine) {
        return new ControleTechnique(1, 1, realisation, resultat, prochaine, null);
    }

    @Test
    void leDelaiDependDuResultat() {
        LocalDate realisation = LocalDate.of(2026, 3, 12);

        assertEquals(LocalDate.of(2028, 3, 12),
                ControleTechniqueService.prochaineDate(realisation, ControleTechnique.Resultat.FAVORABLE));
        assertEquals(LocalDate.of(2026, 5, 12),
                ControleTechniqueService.prochaineDate(realisation, ControleTechnique.Resultat.DEFAVORABLE));
    }

    @Test
    void sansControleLEtatEstAucun() {
        ControleTechniqueService.Suivi suivi = ControleTechniqueService.analyser(List.of());

        assertEquals(ControleTechniqueService.Etat.AUCUN, suivi.getEtat());
        assertNull(suivi.getDernier());
        assertNull(suivi.getDateProchaine());
        assertFalse(suivi.isProche());
    }

    @Test
    void uneEcheanceLointaineEstAjour() {
        ControleTechnique c = controle(AUJOURDHUI.minusYears(1),
                ControleTechnique.Resultat.FAVORABLE, AUJOURDHUI.plusYears(1));

        ControleTechniqueService.Suivi suivi = ControleTechniqueService.analyser(List.of(c));

        assertEquals(ControleTechniqueService.Etat.OK, suivi.getEtat());
        assertEquals(AUJOURDHUI.plusYears(1), suivi.getDateProchaine());
        assertEquals(365, suivi.getJoursRestants());
        assertFalse(suivi.isProche());
    }

    @Test
    void uneEcheanceADeuxMoisExactementEstSignaleeAPrevoir() {
        ControleTechnique c = controle(AUJOURDHUI.minusYears(2),
                ControleTechnique.Resultat.FAVORABLE, AUJOURDHUI.plusMonths(2));

        ControleTechniqueService.Suivi suivi = ControleTechniqueService.analyser(List.of(c));

        assertEquals(ControleTechniqueService.Etat.A_PREVOIR, suivi.getEtat());
        assertTrue(suivi.isProche());
    }

    @Test
    void uneEcheanceJusteApresDeuxMoisResteAjour() {
        ControleTechnique c = controle(AUJOURDHUI.minusYears(2),
                ControleTechnique.Resultat.FAVORABLE, AUJOURDHUI.plusMonths(2).plusDays(1));

        ControleTechniqueService.Suivi suivi = ControleTechniqueService.analyser(List.of(c));

        assertEquals(ControleTechniqueService.Etat.OK, suivi.getEtat());
        assertFalse(suivi.isProche());
    }

    @Test
    void uneEcheanceAujourdhuiEstAPrevoirMaisPasEnRetard() {
        ControleTechnique c = controle(AUJOURDHUI.minusYears(2),
                ControleTechnique.Resultat.FAVORABLE, AUJOURDHUI);

        ControleTechniqueService.Suivi suivi = ControleTechniqueService.analyser(List.of(c));

        assertEquals(ControleTechniqueService.Etat.A_PREVOIR, suivi.getEtat());
        assertEquals(0, suivi.getJoursRestants());
        assertTrue(suivi.isProche());
    }

    @Test
    void uneEcheanceDepasseeEstEnRetard() {
        ControleTechnique c = controle(AUJOURDHUI.minusYears(3),
                ControleTechnique.Resultat.FAVORABLE, AUJOURDHUI.minusDays(5));

        ControleTechniqueService.Suivi suivi = ControleTechniqueService.analyser(List.of(c));

        assertEquals(ControleTechniqueService.Etat.EN_RETARD, suivi.getEtat());
        assertEquals(-5, suivi.getJoursRestants());
        assertTrue(suivi.isProche());
    }

    @Test
    void leControleLePlusRecentDetermineLeProchainControle() {
        ControleTechnique ancien = controle(AUJOURDHUI.minusYears(3),
                ControleTechnique.Resultat.FAVORABLE, AUJOURDHUI.minusYears(1));
        ControleTechnique recent = controle(AUJOURDHUI.minusMonths(1),
                ControleTechnique.Resultat.DEFAVORABLE, AUJOURDHUI.plusMonths(1));

        ControleTechniqueService.Suivi suivi = ControleTechniqueService.analyser(List.of(ancien, recent));

        assertEquals(recent, suivi.getDernier());
        assertEquals(AUJOURDHUI.plusMonths(1), suivi.getDateProchaine());
        assertEquals(ControleTechniqueService.Etat.A_PREVOIR, suivi.getEtat());
    }

    @Test
    void uneContreVisiteProcheEstSignalee() {
        ControleTechnique c = controle(AUJOURDHUI.minusMonths(1),
                ControleTechnique.Resultat.DEFAVORABLE, AUJOURDHUI.plusMonths(1));

        ControleTechniqueService.Suivi suivi = ControleTechniqueService.analyser(List.of(c));

        assertEquals(ControleTechniqueService.Etat.A_PREVOIR, suivi.getEtat());
        assertTrue(suivi.isProche());
    }
}
