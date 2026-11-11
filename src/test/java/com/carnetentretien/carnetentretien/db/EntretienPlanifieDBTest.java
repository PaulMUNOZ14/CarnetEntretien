package com.carnetentretien.carnetentretien.db;

import com.carnetentretien.carnetentretien.BaseBddTest;
import com.carnetentretien.carnetentretien.model.EntretienPlanifie;
import com.carnetentretien.carnetentretien.model.Vehicule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EntretienPlanifieDBTest extends BaseBddTest {

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
    void uneBaseNeuveNeContientAucunePlanification() {
        assertTrue(EntretienPlanifieDB.getTousLesEntretiensPlanifies().isEmpty());
        assertTrue(EntretienPlanifieDB.getEntretiensPlanifiesPourVehicule(vehiculeA).isEmpty());
    }

    @Test
    void unePlanificationAjouteeEstRelitIdentique() {
        LocalDate date = LocalDate.now().plusDays(20);
        EntretienPlanifieDB.ajouterEntretienPlanifie(
                new EntretienPlanifie(0, vehiculeA, "Vidange", date, "Garage Renault"));

        List<EntretienPlanifie> liste = EntretienPlanifieDB.getEntretiensPlanifiesPourVehicule(vehiculeA);

        assertEquals(1, liste.size());
        EntretienPlanifie p = liste.get(0);
        assertTrue(p.getId() > 0);
        assertEquals(vehiculeA, p.getVehiculeId());
        assertEquals("Vidange", p.getTypeOperation());
        assertEquals(date, p.getDatePrevue());
        assertEquals("Garage Renault", p.getNote());
    }

    @Test
    void laRemarqueEstFacultative() {
        EntretienPlanifieDB.ajouterEntretienPlanifie(
                new EntretienPlanifie(0, vehiculeA, "Pneus avant", LocalDate.now().plusMonths(2)));

        EntretienPlanifie p = EntretienPlanifieDB.getEntretiensPlanifiesPourVehicule(vehiculeA).get(0);
        assertNull(p.getNote());
    }

    @Test
    void chaqueVehiculeNeVoitQueSesPropresPlanifications() {
        EntretienPlanifieDB.ajouterEntretienPlanifie(
                new EntretienPlanifie(0, vehiculeA, "Vidange", LocalDate.now().plusDays(1)));
        EntretienPlanifieDB.ajouterEntretienPlanifie(
                new EntretienPlanifie(0, vehiculeA, "Freins", LocalDate.now().plusDays(2)));
        EntretienPlanifieDB.ajouterEntretienPlanifie(
                new EntretienPlanifie(0, vehiculeB, "Pneus", LocalDate.now().plusDays(3)));

        assertEquals(2, EntretienPlanifieDB.getEntretiensPlanifiesPourVehicule(vehiculeA).size());
        List<EntretienPlanifie> deB = EntretienPlanifieDB.getEntretiensPlanifiesPourVehicule(vehiculeB);
        assertEquals(1, deB.size());
        assertEquals("Pneus", deB.get(0).getTypeOperation());
        assertEquals(3, EntretienPlanifieDB.getTousLesEntretiensPlanifies().size());
    }

    @Test
    void modifierMetAJourLaPlanificationCibleSeulement() {
        EntretienPlanifieDB.ajouterEntretienPlanifie(
                new EntretienPlanifie(0, vehiculeA, "Vidange", LocalDate.now().plusDays(1)));
        EntretienPlanifieDB.ajouterEntretienPlanifie(
                new EntretienPlanifie(0, vehiculeA, "Freins", LocalDate.now().plusDays(2)));
        EntretienPlanifie aModifier = EntretienPlanifieDB.getEntretiensPlanifiesPourVehicule(vehiculeA).get(0);

        LocalDate nouvelleDate = LocalDate.now().plusMonths(3);
        EntretienPlanifieDB.modifierEntretienPlanifie(new EntretienPlanifie(
                aModifier.getId(), vehiculeB, "Vidange + filtre", nouvelleDate, "À commander"));

        List<EntretienPlanifie> deB = EntretienPlanifieDB.getEntretiensPlanifiesPourVehicule(vehiculeB);
        assertEquals(1, deB.size());
        EntretienPlanifie modifie = deB.get(0);
        assertEquals(aModifier.getId(), modifie.getId());
        assertEquals(vehiculeB, modifie.getVehiculeId());
        assertEquals("Vidange + filtre", modifie.getTypeOperation());
        assertEquals(nouvelleDate, modifie.getDatePrevue());
        assertEquals("À commander", modifie.getNote());

        List<EntretienPlanifie> restantsA = EntretienPlanifieDB.getEntretiensPlanifiesPourVehicule(vehiculeA);
        assertEquals(1, restantsA.size());
        assertEquals("Freins", restantsA.get(0).getTypeOperation());
    }

    @Test
    void supprimerRetireUniquementLaPlanificationCible() {
        EntretienPlanifieDB.ajouterEntretienPlanifie(
                new EntretienPlanifie(0, vehiculeA, "Vidange", LocalDate.now().plusDays(1)));
        EntretienPlanifieDB.ajouterEntretienPlanifie(
                new EntretienPlanifie(0, vehiculeA, "Freins", LocalDate.now().plusDays(2)));
        EntretienPlanifie aSupprimer = EntretienPlanifieDB.getEntretiensPlanifiesPourVehicule(vehiculeA).get(0);

        EntretienPlanifieDB.supprimerEntretienPlanifie(aSupprimer.getId());

        List<EntretienPlanifie> restants = EntretienPlanifieDB.getEntretiensPlanifiesPourVehicule(vehiculeA);
        assertEquals(1, restants.size());
        assertNotEquals(aSupprimer.getId(), restants.get(0).getId());
    }

    @Test
    void supprimerUnePlanificationInexistanteNeCasseRien() {
        assertDoesNotThrow(() -> EntretienPlanifieDB.supprimerEntretienPlanifie(9999));
    }

    @Test
    void supprimerLesPlanificationsDUneVehiculeNeTouchePasAuxAutres() {
        EntretienPlanifieDB.ajouterEntretienPlanifie(
                new EntretienPlanifie(0, vehiculeA, "Vidange", LocalDate.now().plusDays(1)));
        EntretienPlanifieDB.ajouterEntretienPlanifie(
                new EntretienPlanifie(0, vehiculeB, "Freins", LocalDate.now().plusDays(2)));

        EntretienPlanifieDB.supprimerEntretiensPlanifiesPourVehicule(vehiculeA);

        assertTrue(EntretienPlanifieDB.getEntretiensPlanifiesPourVehicule(vehiculeA).isEmpty());
        assertEquals(1, EntretienPlanifieDB.getEntretiensPlanifiesPourVehicule(vehiculeB).size());
    }
}
