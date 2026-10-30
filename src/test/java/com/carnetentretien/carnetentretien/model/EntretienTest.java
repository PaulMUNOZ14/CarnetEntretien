package com.carnetentretien.carnetentretien.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class EntretienTest {

    @Test
    void lesGettersRenvoientLesValeursDuConstructeur() {
        LocalDate date = LocalDate.of(2026, 3, 15);
        Entretien e = new Entretien(4, 7, "Vidange", date, 52000, 89.9);

        assertEquals(4, e.getId());
        assertEquals(7, e.getVehiculeId());
        assertEquals("Vidange", e.getTypeOperation());
        assertEquals(date, e.getDateRealisation());
        assertEquals(52000, e.getKilometrageRealisation());
        assertEquals(89.9, e.getCout(), 0.0001);
    }

    @Test
    void toStringContientDateOperationKilometrageEtCout() {
        Entretien e = new Entretien(1, 1, "Vidange", LocalDate.of(2026, 3, 15), 52000, 89.9);

        assertEquals("2026-03-15 | Vidange à 52000 km (89.9 €)", e.toString());
    }
}
