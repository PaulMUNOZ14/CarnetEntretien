package com.carnetentretien.carnetentretien.db;

import com.carnetentretien.carnetentretien.BaseBddTest;
import com.carnetentretien.carnetentretien.model.Entretien;
import com.carnetentretien.carnetentretien.model.Vehicule;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VehiculeDBTest extends BaseBddTest {

    private Vehicule ajouterEtRecuperer(Vehicule v) {
        VehiculeDB.ajouterVehicule(v);
        List<Vehicule> tous = VehiculeDB.getTousLesVehicules();
        return tous.get(tous.size() - 1);
    }

    private Vehicule simple(String marque, String modele, int km, int kmAn) {
        return ajouterEtRecuperer(new Vehicule(0, null, marque, modele, km, kmAn));
    }

    @Test
    void uneBaseNeuveNeContientAucunVehicule() {
        assertTrue(VehiculeDB.getTousLesVehicules().isEmpty());
    }

    @Test
    void unVehiculeAjouteEstRelitAvecUnIdentifiantGenere() {
        Vehicule v = ajouterEtRecuperer(new Vehicule(0, "AB-123-CD", "Peugeot", "208", 45000, 12000, "https://x/p.jpg", "abc.png"));

        assertTrue(v.getId() > 0);
        assertEquals("AB-123-CD", v.getImmatriculation());
        assertEquals("Peugeot", v.getMarque());
        assertEquals("208", v.getModele());
        assertEquals(45000, v.getKilometrageActuel());
        assertEquals(12000, v.getKilometrageAnnuelEstime());
        assertEquals("https://x/p.jpg", v.getImageUrl());
        assertEquals("abc.png", v.getImageLocale());
    }

    @Test
    void lesChampsFacultatifsPeuventEtreNull() {
        Vehicule v = simple("Renault", "Clio", 1000, 2000);

        assertNull(v.getImmatriculation());
        assertNull(v.getImageUrl());
        assertNull(v.getImageLocale());
    }

    @Test
    void lesIdentifiantsDeDeuxVehiculesSontDifferents() {
        Vehicule a = simple("A", "1", 1, 1);
        Vehicule b = simple("B", "2", 2, 2);

        assertNotEquals(a.getId(), b.getId());
        assertEquals(2, VehiculeDB.getTousLesVehicules().size());
    }

    @Test
    void modifierMetAJourTousLesChampsDuBonVehiculeSeulement() {
        Vehicule a = simple("A", "1", 100, 200);
        Vehicule b = simple("B", "2", 300, 400);

        a.setImmatriculation("ZZ-999-ZZ");
        a.setMarque("Citroën");
        a.setModele("C3");
        a.setKilometrageActuel(150);
        a.setKilometrageAnnuelEstime(250);
        a.setImageUrl("https://x/c3.jpg");
        a.setImageLocale("c3.png");
        VehiculeDB.modifierVehicule(a);

        List<Vehicule> tous = VehiculeDB.getTousLesVehicules();
        Vehicule aRelu = tous.stream().filter(v -> v.getId() == a.getId()).findFirst().orElseThrow();
        Vehicule bRelu = tous.stream().filter(v -> v.getId() == b.getId()).findFirst().orElseThrow();

        assertEquals("ZZ-999-ZZ", aRelu.getImmatriculation());
        assertEquals("Citroën", aRelu.getMarque());
        assertEquals("C3", aRelu.getModele());
        assertEquals(150, aRelu.getKilometrageActuel());
        assertEquals(250, aRelu.getKilometrageAnnuelEstime());
        assertEquals("https://x/c3.jpg", aRelu.getImageUrl());
        assertEquals("c3.png", aRelu.getImageLocale());
        assertEquals("B", bRelu.getMarque());
        assertEquals(300, bRelu.getKilometrageActuel());
        assertNull(bRelu.getImageLocale());
    }

    @Test
    void onPeutRetirerLaPhotoPersonnelleEnRemettantNull() {
        Vehicule v = ajouterEtRecuperer(new Vehicule(0, null, "Renault", "Clio", 1000, 2000, "https://x/p.jpg", "abc.png"));

        v.setImageLocale(null);
        VehiculeDB.modifierVehicule(v);

        Vehicule relu = VehiculeDB.getTousLesVehicules().get(0);
        assertNull(relu.getImageLocale());
        assertEquals("https://x/p.jpg", relu.getImageUrl());   // la photo trouvée via la plaque reste intacte
    }

    @Test
    void supprimerRetireLeVehiculeEtSonHistoriqueSansToucherLesAutres() {
        Vehicule a = simple("A", "1", 100, 200);
        Vehicule b = simple("B", "2", 300, 400);
        EntretienDB.ajouterEntretien(new Entretien(0, a.getId(), "Vidange", LocalDate.of(2026, 1, 10), 90, 50));
        EntretienDB.ajouterEntretien(new Entretien(0, b.getId(), "Freins", LocalDate.of(2026, 2, 10), 290, 120));

        VehiculeDB.supprimerVehicule(a.getId());

        List<Vehicule> restants = VehiculeDB.getTousLesVehicules();
        assertEquals(1, restants.size());
        assertEquals(b.getId(), restants.get(0).getId());
        assertTrue(EntretienDB.getEntretiensPourVehicule(a.getId()).isEmpty());
        assertEquals(1, EntretienDB.getEntretiensPourVehicule(b.getId()).size());
    }

    @Test
    void supprimerUnVehiculeInexistantNeCasseRien() {
        simple("A", "1", 100, 200);

        assertDoesNotThrow(() -> VehiculeDB.supprimerVehicule(9999));

        assertEquals(1, VehiculeDB.getTousLesVehicules().size());
    }

    @Test
    void lesApostrophesEtLInjectionSQLSontStockeesTellesQuelles() {
        Vehicule v = simple("Citroën", "L'Ami; DROP TABLE Vehicule;--", 1, 1);

        assertEquals("L'Ami; DROP TABLE Vehicule;--", v.getModele());
        assertEquals(1, VehiculeDB.getTousLesVehicules().size());
    }
}
