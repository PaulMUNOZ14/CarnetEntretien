package com.carnetentretien.carnetentretien.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PreconisationTest {

    @Test
    void lesGettersRenvoientLesValeursDuConstructeur() {
        Preconisation p = new Preconisation(3, "Courroie de distribution", 120000, 72);

        assertEquals(3, p.getId());
        assertEquals("Courroie de distribution", p.getTypeOperation());
        assertEquals(120000, p.getIntervalleKm());
        assertEquals(72, p.getIntervalleMois());
    }

    @Test
    void toStringDecritLIntervalle() {
        assertEquals("Vidange (Tous les 15000 km ou 12 mois)", new Preconisation(1, "Vidange", 15000, 12).toString());
    }
}
