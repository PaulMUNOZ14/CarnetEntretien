package com.carnetentretien.carnetentretien.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ReleveKilometrageTest {

    @Test
    void constructeurCompletConserveTousLesChamps() {
        LocalDate date = LocalDate.of(2026, 3, 12);
        ReleveKilometrage r = new ReleveKilometrage(3, 7, date, 45000, "Contrôle du compteur");

        assertEquals(3, r.getId());
        assertEquals(7, r.getVehiculeId());
        assertEquals(date, r.getDateReleve());
        assertEquals(45000, r.getKilometrage());
        assertEquals("Contrôle du compteur", r.getNote());
    }

    @Test
    void constructeurSansNoteLaisseLaNoteNull() {
        ReleveKilometrage r = new ReleveKilometrage(1, 2, LocalDate.now(), 1000);

        assertNull(r.getNote());
    }

    @Test
    void toStringAfficheLaDateEtLeKilometrage() {
        ReleveKilometrage r = new ReleveKilometrage(
                1, 2, LocalDate.of(2026, 3, 12), 45000, "Plein");

        assertEquals("2026-03-12 | 45000 km (Plein)", r.toString());
    }
}
