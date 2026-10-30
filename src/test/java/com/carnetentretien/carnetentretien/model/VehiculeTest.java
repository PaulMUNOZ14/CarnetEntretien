package com.carnetentretien.carnetentretien.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VehiculeTest {

    @Test
    void constructeurCompletConserveTousLesChamps() {
        Vehicule v = new Vehicule(3, "AB-123-CD", "Peugeot", "208", 45000, 12000, "https://x/p.jpg", "abc.png");

        assertEquals(3, v.getId());
        assertEquals("AB-123-CD", v.getImmatriculation());
        assertEquals("Peugeot", v.getMarque());
        assertEquals("208", v.getModele());
        assertEquals(45000, v.getKilometrageActuel());
        assertEquals(12000, v.getKilometrageAnnuelEstime());
        assertEquals("https://x/p.jpg", v.getImageUrl());
        assertEquals("abc.png", v.getImageLocale());
    }

    @Test
    void constructeurSansImageLocaleLaisseImageLocaleNull() {
        Vehicule v = new Vehicule(1, "AB-123-CD", "Renault", "Clio", 10, 20, "https://x/p.jpg");

        assertEquals("https://x/p.jpg", v.getImageUrl());
        assertNull(v.getImageLocale());
    }

    @Test
    void constructeurSansAucuneImageLaisseLesDeuxNull() {
        Vehicule v = new Vehicule(1, null, "Renault", "Clio", 10, 20);

        assertNull(v.getImmatriculation());
        assertNull(v.getImageUrl());
        assertNull(v.getImageLocale());
    }

    @Test
    void lesSettersModifientLesValeurs() {
        Vehicule v = new Vehicule(1, "AA-111-AA", "A", "B", 1, 2);

        v.setId(9);
        v.setImmatriculation("ZZ-999-ZZ");
        v.setMarque("Citroën");
        v.setModele("C3");
        v.setKilometrageActuel(123456);
        v.setKilometrageAnnuelEstime(15000);
        v.setImageUrl("https://x/c3.jpg");
        v.setImageLocale("c3.png");

        assertEquals(9, v.getId());
        assertEquals("ZZ-999-ZZ", v.getImmatriculation());
        assertEquals("Citroën", v.getMarque());
        assertEquals("C3", v.getModele());
        assertEquals(123456, v.getKilometrageActuel());
        assertEquals(15000, v.getKilometrageAnnuelEstime());
        assertEquals("https://x/c3.jpg", v.getImageUrl());
        assertEquals("c3.png", v.getImageLocale());
    }

    @Test
    void toStringAfficheMarqueModeleEtKilometrage() {
        assertEquals("Peugeot 208 - 45000 km", new Vehicule(1, null, "Peugeot", "208", 45000, 12000).toString());
    }
}
