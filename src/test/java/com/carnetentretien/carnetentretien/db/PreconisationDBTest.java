package com.carnetentretien.carnetentretien.db;

import com.carnetentretien.carnetentretien.BaseBddTest;
import com.carnetentretien.carnetentretien.model.Preconisation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PreconisationDBTest extends BaseBddTest {

    @Test
    void uneBaseNeuveNAAucuneRegle() {
        assertTrue(PreconisationDB.getToutesLesPreconisations().isEmpty());
    }

    @Test
    void uneRegleAjouteeEstRelue() {
        PreconisationDB.ajouterPreconisation(new Preconisation(0, "Vidange", 15000, 12));

        List<Preconisation> regles = PreconisationDB.getToutesLesPreconisations();

        assertEquals(1, regles.size());
        Preconisation p = regles.get(0);
        assertTrue(p.getId() > 0);
        assertEquals("Vidange", p.getTypeOperation());
        assertEquals(15000, p.getIntervalleKm());
        assertEquals(12, p.getIntervalleMois());
    }

    @Test
    void modifierMetAJourLaBonneRegleSeulement() {
        PreconisationDB.ajouterPreconisation(new Preconisation(0, "Vidange", 15000, 12));
        PreconisationDB.ajouterPreconisation(new Preconisation(0, "Freins", 40000, 48));
        Preconisation vidange = PreconisationDB.getToutesLesPreconisations().get(0);

        PreconisationDB.modifierPreconisation(new Preconisation(vidange.getId(), "Vidange moteur", 10000, 6));

        List<Preconisation> regles = PreconisationDB.getToutesLesPreconisations();
        Preconisation modifiee = regles.stream().filter(p -> p.getId() == vidange.getId()).findFirst().orElseThrow();
        Preconisation autre = regles.stream().filter(p -> p.getId() != vidange.getId()).findFirst().orElseThrow();

        assertEquals("Vidange moteur", modifiee.getTypeOperation());
        assertEquals(10000, modifiee.getIntervalleKm());
        assertEquals(6, modifiee.getIntervalleMois());
        assertEquals("Freins", autre.getTypeOperation());
        assertEquals(40000, autre.getIntervalleKm());
    }

    @Test
    void supprimerRetireUniquementLaRegleCible() {
        PreconisationDB.ajouterPreconisation(new Preconisation(0, "Vidange", 15000, 12));
        PreconisationDB.ajouterPreconisation(new Preconisation(0, "Freins", 40000, 48));
        Preconisation vidange = PreconisationDB.getToutesLesPreconisations().get(0);

        PreconisationDB.supprimerPreconisation(vidange.getId());

        List<Preconisation> restantes = PreconisationDB.getToutesLesPreconisations();
        assertEquals(1, restantes.size());
        assertEquals("Freins", restantes.get(0).getTypeOperation());
    }

    @Test
    void supprimerUneRegleInexistanteNeCasseRien() {
        assertDoesNotThrow(() -> PreconisationDB.supprimerPreconisation(9999));
    }
}
