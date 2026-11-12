package com.carnetentretien.carnetentretien.db;

import com.carnetentretien.carnetentretien.BaseBddTest;
import com.carnetentretien.carnetentretien.model.ReleveKilometrage;
import com.carnetentretien.carnetentretien.model.Vehicule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReleveKilometrageDBTest extends BaseBddTest {

    private int vehiculeA;
    private int vehiculeB;

    @BeforeEach
    void creerDeuxVehicules() {
        VehiculeDB.ajouterVehicule(new Vehicule(0, null, "Peugeot", "208", 45000, 12000));
        VehiculeDB.ajouterVehicule(new Vehicule(0, null, "Renault", "Clio", 80000, 10000));
        List<Vehicule> tous = VehiculeDB.getTousLesVehicules();
        vehiculeA = tous.get(0).getId();
        vehiculeB = tous.get(1).getId();

        // On repart d'un historique vide pour chaque test (les véhicules ont un relevé initial).
        ReleveKilometrageDB.supprimerRelevesPourVehicule(vehiculeA);
        ReleveKilometrageDB.supprimerRelevesPourVehicule(vehiculeB);
    }

    private static ReleveKilometrage releve(int vehiculeId, LocalDate date, int kilometrage, String note) {
        return new ReleveKilometrage(0, vehiculeId, date, kilometrage, note);
    }

    private int kilometrageActuel(int vehiculeId) {
        return VehiculeDB.getTousLesVehicules().stream()
                .filter(v -> v.getId() == vehiculeId)
                .findFirst()
                .orElseThrow()
                .getKilometrageActuel();
    }

    @Test
    void uneBaseNeuveNeContientAucunReleve() {
        assertTrue(ReleveKilometrageDB.getTousLesReleves().isEmpty());
        assertTrue(ReleveKilometrageDB.getRelevesPourVehicule(vehiculeA).isEmpty());
    }

    @Test
    void unReleveAjouteEstRelitIdentique() {
        LocalDate date = LocalDate.of(2026, 3, 12);
        ReleveKilometrageDB.ajouterReleve(releve(vehiculeA, date, 52000, "Après un long trajet"));

        List<ReleveKilometrage> liste = ReleveKilometrageDB.getRelevesPourVehicule(vehiculeA);

        assertEquals(1, liste.size());
        ReleveKilometrage r = liste.get(0);
        assertTrue(r.getId() > 0);
        assertEquals(vehiculeA, r.getVehiculeId());
        assertEquals(date, r.getDateReleve());
        assertEquals(52000, r.getKilometrage());
        assertEquals("Après un long trajet", r.getNote());
    }

    @Test
    void laRemarqueEstFacultative() {
        ReleveKilometrageDB.ajouterReleve(releve(vehiculeA, LocalDate.now(), 50000, null));

        ReleveKilometrage r = ReleveKilometrageDB.getRelevesPourVehicule(vehiculeA).get(0);
        assertNull(r.getNote());
    }

    @Test
    void chaqueVehiculeNeVoitQueSesPropresReleves() {
        ReleveKilometrageDB.ajouterReleve(releve(vehiculeA, LocalDate.of(2026, 3, 1), 50000, null));
        ReleveKilometrageDB.ajouterReleve(releve(vehiculeB, LocalDate.of(2026, 3, 2), 90000, null));

        assertEquals(1, ReleveKilometrageDB.getRelevesPourVehicule(vehiculeA).size());
        assertEquals(1, ReleveKilometrageDB.getRelevesPourVehicule(vehiculeB).size());
        assertEquals(2, ReleveKilometrageDB.getTousLesReleves().size());
    }

    @Test
    void lesRelevesSontDuPlusRecentAuPlusAncien() {
        ReleveKilometrageDB.ajouterReleve(releve(vehiculeA, LocalDate.of(2026, 1, 10), 48000, null));
        ReleveKilometrageDB.ajouterReleve(releve(vehiculeA, LocalDate.of(2026, 3, 10), 51000, null));
        ReleveKilometrageDB.ajouterReleve(releve(vehiculeA, LocalDate.of(2026, 3, 10), 51500, null));

        List<ReleveKilometrage> liste = ReleveKilometrageDB.getRelevesPourVehicule(vehiculeA);

        assertEquals(3, liste.size());
        assertEquals(51500, liste.get(0).getKilometrage());   // même date : le dernier saisi d'abord
        assertEquals(51000, liste.get(1).getKilometrage());
        assertEquals(48000, liste.get(2).getKilometrage());
    }

    @Test
    void ajouterUnReleveRecentMetAJourLeKilometrageActuel() {
        ReleveKilometrageDB.ajouterReleve(releve(vehiculeA, LocalDate.of(2026, 3, 10), 60000, null));

        assertEquals(60000, kilometrageActuel(vehiculeA));
        assertEquals(80000, kilometrageActuel(vehiculeB));   // l'autre véhicule ne bouge pas
    }

    @Test
    void ajouterUnReleveAncienNeChangePasLeKilometrageActuel() {
        ReleveKilometrageDB.ajouterReleve(releve(vehiculeA, LocalDate.of(2026, 3, 10), 60000, null));
        ReleveKilometrageDB.ajouterReleve(releve(vehiculeA, LocalDate.of(2026, 3, 1), 55000, null));

        assertEquals(60000, kilometrageActuel(vehiculeA));
    }

    @Test
    void modifierUnReleveMetAJourLaValeurEtLeKilometrageActuel() {
        ReleveKilometrageDB.ajouterReleve(releve(vehiculeA, LocalDate.of(2026, 3, 10), 60000, "Ancienne valeur"));
        ReleveKilometrage original = ReleveKilometrageDB.getRelevesPourVehicule(vehiculeA).get(0);

        ReleveKilometrageDB.modifierReleve(new ReleveKilometrage(
                original.getId(), vehiculeA, LocalDate.of(2026, 3, 11), 62000, "Valeur corrigée"));

        ReleveKilometrage modifie = ReleveKilometrageDB.getRelevesPourVehicule(vehiculeA).get(0);
        assertEquals(original.getId(), modifie.getId());
        assertEquals(LocalDate.of(2026, 3, 11), modifie.getDateReleve());
        assertEquals(62000, modifie.getKilometrage());
        assertEquals("Valeur corrigée", modifie.getNote());
        assertEquals(62000, kilometrageActuel(vehiculeA));
    }

    @Test
    void supprimerLeDernierReleveRevientAuPrecedent() {
        ReleveKilometrageDB.ajouterReleve(releve(vehiculeA, LocalDate.of(2026, 3, 1), 50000, null));
        ReleveKilometrageDB.ajouterReleve(releve(vehiculeA, LocalDate.of(2026, 3, 10), 60000, null));
        ReleveKilometrage dernier = ReleveKilometrageDB.getRelevesPourVehicule(vehiculeA).get(0);

        ReleveKilometrageDB.supprimerReleve(dernier.getId());

        assertEquals(1, ReleveKilometrageDB.getRelevesPourVehicule(vehiculeA).size());
        assertEquals(50000, kilometrageActuel(vehiculeA));
    }

    @Test
    void supprimerTousLesRelevesDUneVehiculeNeTouchePasAuxAutres() {
        ReleveKilometrageDB.ajouterReleve(releve(vehiculeA, LocalDate.of(2026, 3, 1), 50000, null));
        ReleveKilometrageDB.ajouterReleve(releve(vehiculeB, LocalDate.of(2026, 3, 1), 90000, null));

        ReleveKilometrageDB.supprimerRelevesPourVehicule(vehiculeA);

        assertTrue(ReleveKilometrageDB.getRelevesPourVehicule(vehiculeA).isEmpty());
        assertEquals(1, ReleveKilometrageDB.getRelevesPourVehicule(vehiculeB).size());
    }

    @Test
    void supprimerUnReleveInexistantNeCasseRien() {
        assertDoesNotThrow(() -> ReleveKilometrageDB.supprimerReleve(9999));
    }
}
