package com.carnetentretien.carnetentretien.db;

import com.carnetentretien.carnetentretien.BaseBddTest;
import com.carnetentretien.carnetentretien.model.ControleTechniquePlanifie;
import com.carnetentretien.carnetentretien.model.Vehicule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ControleTechniquePlanifieDBTest extends BaseBddTest {

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
        assertTrue(ControleTechniquePlanifieDB.getTousLesControlesPlanifies().isEmpty());
        assertTrue(ControleTechniquePlanifieDB.getControlesPlanifiesPourVehicule(vehiculeA).isEmpty());
    }

    @Test
    void unePlanificationAjouteeEstRelitIdentique() {
        LocalDate date = LocalDate.now().plusMonths(2);
        ControleTechniquePlanifieDB.ajouterControleTechniquePlanifie(
                new ControleTechniquePlanifie(0, vehiculeA, date, "Centre de contrôle"));

        List<ControleTechniquePlanifie> liste = ControleTechniquePlanifieDB.getControlesPlanifiesPourVehicule(vehiculeA);

        assertEquals(1, liste.size());
        ControleTechniquePlanifie p = liste.get(0);
        assertTrue(p.getId() > 0);
        assertEquals(vehiculeA, p.getVehiculeId());
        assertEquals(date, p.getDatePrevue());
        assertEquals("Centre de contrôle", p.getNote());
    }

    @Test
    void laRemarqueEstFacultative() {
        ControleTechniquePlanifieDB.ajouterControleTechniquePlanifie(
                new ControleTechniquePlanifie(0, vehiculeA, LocalDate.now().plusMonths(2)));

        ControleTechniquePlanifie p = ControleTechniquePlanifieDB.getControlesPlanifiesPourVehicule(vehiculeA).get(0);
        assertNull(p.getNote());
    }

    @Test
    void chaqueVehiculeNeVoitQueSesPropresPlanifications() {
        ControleTechniquePlanifieDB.ajouterControleTechniquePlanifie(
                new ControleTechniquePlanifie(0, vehiculeA, LocalDate.now().plusDays(10)));
        ControleTechniquePlanifieDB.ajouterControleTechniquePlanifie(
                new ControleTechniquePlanifie(0, vehiculeB, LocalDate.now().plusDays(20)));

        assertEquals(1, ControleTechniquePlanifieDB.getControlesPlanifiesPourVehicule(vehiculeA).size());
        assertEquals(1, ControleTechniquePlanifieDB.getControlesPlanifiesPourVehicule(vehiculeB).size());
        assertEquals(2, ControleTechniquePlanifieDB.getTousLesControlesPlanifies().size());
    }

    @Test
    void modifierMetAJourLaPlanificationCibleSeulement() {
        ControleTechniquePlanifieDB.ajouterControleTechniquePlanifie(
                new ControleTechniquePlanifie(0, vehiculeA, LocalDate.now().plusDays(1)));
        ControleTechniquePlanifieDB.ajouterControleTechniquePlanifie(
                new ControleTechniquePlanifie(0, vehiculeA, LocalDate.now().plusDays(2)));
        ControleTechniquePlanifie aModifier = ControleTechniquePlanifieDB.getControlesPlanifiesPourVehicule(vehiculeA).get(0);

        LocalDate nouvelleDate = LocalDate.now().plusMonths(3);
        ControleTechniquePlanifieDB.modifierControleTechniquePlanifie(new ControleTechniquePlanifie(
                aModifier.getId(), vehiculeB, nouvelleDate, "Contre-visite"));

        List<ControleTechniquePlanifie> deB = ControleTechniquePlanifieDB.getControlesPlanifiesPourVehicule(vehiculeB);
        assertEquals(1, deB.size());
        ControleTechniquePlanifie modifie = deB.get(0);
        assertEquals(aModifier.getId(), modifie.getId());
        assertEquals(vehiculeB, modifie.getVehiculeId());
        assertEquals(nouvelleDate, modifie.getDatePrevue());
        assertEquals("Contre-visite", modifie.getNote());

        assertEquals(1, ControleTechniquePlanifieDB.getControlesPlanifiesPourVehicule(vehiculeA).size());
    }

    @Test
    void supprimerRetireUniquementLaPlanificationCible() {
        ControleTechniquePlanifieDB.ajouterControleTechniquePlanifie(
                new ControleTechniquePlanifie(0, vehiculeA, LocalDate.now().plusDays(1)));
        ControleTechniquePlanifieDB.ajouterControleTechniquePlanifie(
                new ControleTechniquePlanifie(0, vehiculeA, LocalDate.now().plusDays(2)));
        ControleTechniquePlanifie aSupprimer = ControleTechniquePlanifieDB.getControlesPlanifiesPourVehicule(vehiculeA).get(0);

        ControleTechniquePlanifieDB.supprimerControleTechniquePlanifie(aSupprimer.getId());

        List<ControleTechniquePlanifie> restants = ControleTechniquePlanifieDB.getControlesPlanifiesPourVehicule(vehiculeA);
        assertEquals(1, restants.size());
        assertNotEquals(aSupprimer.getId(), restants.get(0).getId());
    }

    @Test
    void supprimerUnePlanificationInexistanteNeCasseRien() {
        assertDoesNotThrow(() -> ControleTechniquePlanifieDB.supprimerControleTechniquePlanifie(9999));
    }

    @Test
    void supprimerLesPlanificationsDUneVehiculeNeTouchePasAuxAutres() {
        ControleTechniquePlanifieDB.ajouterControleTechniquePlanifie(
                new ControleTechniquePlanifie(0, vehiculeA, LocalDate.now().plusDays(1)));
        ControleTechniquePlanifieDB.ajouterControleTechniquePlanifie(
                new ControleTechniquePlanifie(0, vehiculeB, LocalDate.now().plusDays(2)));

        ControleTechniquePlanifieDB.supprimerControlesPlanifiesPourVehicule(vehiculeA);

        assertTrue(ControleTechniquePlanifieDB.getControlesPlanifiesPourVehicule(vehiculeA).isEmpty());
        assertEquals(1, ControleTechniquePlanifieDB.getControlesPlanifiesPourVehicule(vehiculeB).size());
    }
}
