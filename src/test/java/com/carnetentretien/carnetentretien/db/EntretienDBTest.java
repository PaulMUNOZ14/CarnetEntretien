package com.carnetentretien.carnetentretien.db;

import com.carnetentretien.carnetentretien.BaseBddTest;
import com.carnetentretien.carnetentretien.model.Entretien;
import com.carnetentretien.carnetentretien.model.Vehicule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EntretienDBTest extends BaseBddTest {

    private int vehiculeA;
    private int vehiculeB;

    @BeforeEach
    void creerDeuxVehicules() {
        VehiculeDB.ajouterVehicule(new Vehicule(0, null, "Peugeot", "208", 45000, 12000));
        VehiculeDB.ajouterVehicule(new Vehicule(0, null, "Renault", "Clio", 80000, 10000));
        List<Vehicule> tous = VehiculeDB.getTousLesVehicules();
        vehiculeA = tous.get(0).getId();
        vehiculeB = tous.get(1).getId();
    }

    @Test
    void unVehiculeSansEntretienADesHistoriqueVide() {
        assertTrue(EntretienDB.getEntretiensPourVehicule(vehiculeA).isEmpty());
    }

    @Test
    void unEntretienAjouteEstRelitIdentique() {
        LocalDate date = LocalDate.of(2026, 3, 15);
        EntretienDB.ajouterEntretien(new Entretien(0, vehiculeA, "Vidange", date, 52000, 89.9));

        List<Entretien> liste = EntretienDB.getEntretiensPourVehicule(vehiculeA);

        assertEquals(1, liste.size());
        Entretien e = liste.get(0);
        assertTrue(e.getId() > 0);
        assertEquals(vehiculeA, e.getVehiculeId());
        assertEquals("Vidange", e.getTypeOperation());
        assertEquals(date, e.getDateRealisation());
        assertEquals(52000, e.getKilometrageRealisation());
        assertEquals(89.9, e.getCout(), 0.0001);
    }

    @Test
    void lesDatesLimitesSontConserveesSansDecalage() {
        LocalDate finAnnee = LocalDate.of(2025, 12, 31);
        LocalDate bissextile = LocalDate.of(2024, 2, 29);
        EntretienDB.ajouterEntretien(new Entretien(0, vehiculeA, "A", finAnnee, 1, 1));
        EntretienDB.ajouterEntretien(new Entretien(0, vehiculeA, "B", bissextile, 1, 1));

        List<Entretien> liste = EntretienDB.getEntretiensPourVehicule(vehiculeA);

        assertTrue(liste.stream().anyMatch(e -> e.getDateRealisation().equals(finAnnee)));
        assertTrue(liste.stream().anyMatch(e -> e.getDateRealisation().equals(bissextile)));
    }

    @Test
    void chaqueVehiculeNeVoitQueSesPropresEntretiens() {
        EntretienDB.ajouterEntretien(new Entretien(0, vehiculeA, "Vidange", LocalDate.of(2026, 1, 1), 100, 10));
        EntretienDB.ajouterEntretien(new Entretien(0, vehiculeA, "Freins", LocalDate.of(2026, 2, 1), 200, 20));
        EntretienDB.ajouterEntretien(new Entretien(0, vehiculeB, "Pneus", LocalDate.of(2026, 3, 1), 300, 30));

        assertEquals(2, EntretienDB.getEntretiensPourVehicule(vehiculeA).size());
        List<Entretien> deB = EntretienDB.getEntretiensPourVehicule(vehiculeB);
        assertEquals(1, deB.size());
        assertEquals("Pneus", deB.get(0).getTypeOperation());
    }

    @Test
    void modifierMetAJourLEntretienCibleSeulement() {
        EntretienDB.ajouterEntretien(new Entretien(0, vehiculeA, "Vidange", LocalDate.of(2026, 1, 1), 100, 10));
        EntretienDB.ajouterEntretien(new Entretien(0, vehiculeA, "Freins", LocalDate.of(2026, 2, 1), 200, 20));
        Entretien aModifier = EntretienDB.getEntretiensPourVehicule(vehiculeA).get(0);

        EntretienDB.modifierEntretien(new Entretien(aModifier.getId(), vehiculeA, "Vidange + filtre",
                LocalDate.of(2026, 4, 4), 150, 99.5));

        List<Entretien> liste = EntretienDB.getEntretiensPourVehicule(vehiculeA);
        Entretien modifie = liste.stream().filter(e -> e.getId() == aModifier.getId()).findFirst().orElseThrow();
        Entretien autre = liste.stream().filter(e -> e.getId() != aModifier.getId()).findFirst().orElseThrow();

        assertEquals("Vidange + filtre", modifie.getTypeOperation());
        assertEquals(LocalDate.of(2026, 4, 4), modifie.getDateRealisation());
        assertEquals(150, modifie.getKilometrageRealisation());
        assertEquals(99.5, modifie.getCout(), 0.0001);
        assertEquals("Freins", autre.getTypeOperation());
    }

    @Test
    void supprimerRetireUniquementLEntretienCible() {
        EntretienDB.ajouterEntretien(new Entretien(0, vehiculeA, "Vidange", LocalDate.of(2026, 1, 1), 100, 10));
        EntretienDB.ajouterEntretien(new Entretien(0, vehiculeA, "Freins", LocalDate.of(2026, 2, 1), 200, 20));
        Entretien aSupprimer = EntretienDB.getEntretiensPourVehicule(vehiculeA).get(0);

        EntretienDB.supprimerEntretien(aSupprimer.getId());

        List<Entretien> restants = EntretienDB.getEntretiensPourVehicule(vehiculeA);
        assertEquals(1, restants.size());
        assertNotEquals(aSupprimer.getId(), restants.get(0).getId());
    }

    @Test
    void supprimerUnEntretienInexistantNeCasseRien() {
        assertDoesNotThrow(() -> EntretienDB.supprimerEntretien(9999));
    }
}
