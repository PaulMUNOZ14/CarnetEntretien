package com.carnetentretien.carnetentretien.db;

import com.carnetentretien.carnetentretien.BaseBddTest;
import com.carnetentretien.carnetentretien.model.ControleTechnique;
import com.carnetentretien.carnetentretien.model.Vehicule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ControleTechniqueDBTest extends BaseBddTest {

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

    private static ControleTechnique controle(int vehiculeId, LocalDate realisation,
                                              ControleTechnique.Resultat resultat, LocalDate prochaine,
                                              String note) {
        return new ControleTechnique(0, vehiculeId, realisation, resultat, prochaine, note);
    }

    @Test
    void uneBaseNeuveNeContientAucunControle() {
        assertTrue(ControleTechniqueDB.getTousLesControles().isEmpty());
        assertTrue(ControleTechniqueDB.getControlesPourVehicule(vehiculeA).isEmpty());
    }

    @Test
    void unControleAjouteEstRelitIdentique() {
        LocalDate realisation = LocalDate.of(2026, 3, 12);
        LocalDate prochaine = LocalDate.of(2028, 3, 12);
        ControleTechniqueDB.ajouterControleTechnique(
                controle(vehiculeA, realisation, ControleTechnique.Resultat.FAVORABLE, prochaine, "RAS"));

        List<ControleTechnique> liste = ControleTechniqueDB.getControlesPourVehicule(vehiculeA);

        assertEquals(1, liste.size());
        ControleTechnique c = liste.get(0);
        assertTrue(c.getId() > 0);
        assertEquals(vehiculeA, c.getVehiculeId());
        assertEquals(realisation, c.getDateRealisation());
        assertEquals(ControleTechnique.Resultat.FAVORABLE, c.getResultat());
        assertEquals(prochaine, c.getDateProchaine());
        assertEquals("RAS", c.getNote());
    }

    @Test
    void laRemarqueEstFacultative() {
        ControleTechniqueDB.ajouterControleTechnique(
                controle(vehiculeA, LocalDate.now(), ControleTechnique.Resultat.DEFAVORABLE,
                        LocalDate.now().plusMonths(2), null));

        ControleTechnique c = ControleTechniqueDB.getControlesPourVehicule(vehiculeA).get(0);
        assertNull(c.getNote());
        assertEquals(ControleTechnique.Resultat.DEFAVORABLE, c.getResultat());
    }

    @Test
    void chaqueVehiculeNeVoitQueSesPropresControles() {
        ControleTechniqueDB.ajouterControleTechnique(
                controle(vehiculeA, LocalDate.of(2025, 3, 1), ControleTechnique.Resultat.FAVORABLE,
                        LocalDate.of(2027, 3, 1), null));
        ControleTechniqueDB.ajouterControleTechnique(
                controle(vehiculeB, LocalDate.of(2025, 6, 1), ControleTechnique.Resultat.DEFAVORABLE,
                        LocalDate.of(2025, 8, 1), null));

        List<ControleTechnique> deA = ControleTechniqueDB.getControlesPourVehicule(vehiculeA);
        assertEquals(1, deA.size());
        assertEquals(ControleTechnique.Resultat.FAVORABLE, deA.get(0).getResultat());

        List<ControleTechnique> deB = ControleTechniqueDB.getControlesPourVehicule(vehiculeB);
        assertEquals(1, deB.size());
        assertEquals(ControleTechnique.Resultat.DEFAVORABLE, deB.get(0).getResultat());

        assertEquals(2, ControleTechniqueDB.getTousLesControles().size());
    }

    @Test
    void lesControlesSontDuPlusRecentAuPlusAncien() {
        ControleTechniqueDB.ajouterControleTechnique(
                controle(vehiculeA, LocalDate.of(2024, 3, 1), ControleTechnique.Resultat.FAVORABLE,
                        LocalDate.of(2026, 3, 1), null));
        ControleTechniqueDB.ajouterControleTechnique(
                controle(vehiculeA, LocalDate.of(2026, 3, 1), ControleTechnique.Resultat.FAVORABLE,
                        LocalDate.of(2028, 3, 1), null));

        List<ControleTechnique> liste = ControleTechniqueDB.getControlesPourVehicule(vehiculeA);
        assertEquals(2, liste.size());
        assertEquals(LocalDate.of(2026, 3, 1), liste.get(0).getDateRealisation());
        assertEquals(LocalDate.of(2024, 3, 1), liste.get(1).getDateRealisation());
    }

    @Test
    void modifierMetAJourLeControleCibleSeulement() {
        ControleTechniqueDB.ajouterControleTechnique(
                controle(vehiculeA, LocalDate.of(2025, 3, 1), ControleTechnique.Resultat.FAVORABLE,
                        LocalDate.of(2027, 3, 1), null));
        ControleTechniqueDB.ajouterControleTechnique(
                controle(vehiculeA, LocalDate.of(2025, 6, 1), ControleTechnique.Resultat.DEFAVORABLE,
                        LocalDate.of(2025, 8, 1), null));
        ControleTechnique aModifier = ControleTechniqueDB.getControlesPourVehicule(vehiculeA).get(0);

        LocalDate nouvelleProchaine = LocalDate.of(2025, 9, 15);
        ControleTechniqueDB.modifierControleTechnique(new ControleTechnique(
                aModifier.getId(), vehiculeB, aModifier.getDateRealisation(),
                ControleTechnique.Resultat.FAVORABLE, nouvelleProchaine, "Contre-visite passée"));

        List<ControleTechnique> deB = ControleTechniqueDB.getControlesPourVehicule(vehiculeB);
        assertEquals(1, deB.size());
        ControleTechnique modifie = deB.get(0);
        assertEquals(aModifier.getId(), modifie.getId());
        assertEquals(vehiculeB, modifie.getVehiculeId());
        assertEquals(aModifier.getDateRealisation(), modifie.getDateRealisation());
        assertEquals(ControleTechnique.Resultat.FAVORABLE, modifie.getResultat());
        assertEquals(nouvelleProchaine, modifie.getDateProchaine());
        assertEquals("Contre-visite passée", modifie.getNote());

        List<ControleTechnique> restantsA = ControleTechniqueDB.getControlesPourVehicule(vehiculeA);
        assertEquals(1, restantsA.size());
        assertEquals(ControleTechnique.Resultat.FAVORABLE, restantsA.get(0).getResultat());
        assertNotEquals(aModifier.getId(), restantsA.get(0).getId());
    }

    @Test
    void supprimerRetireUniquementLeControleCible() {
        ControleTechniqueDB.ajouterControleTechnique(
                controle(vehiculeA, LocalDate.of(2025, 3, 1), ControleTechnique.Resultat.FAVORABLE,
                        LocalDate.of(2027, 3, 1), null));
        ControleTechniqueDB.ajouterControleTechnique(
                controle(vehiculeA, LocalDate.of(2025, 6, 1), ControleTechnique.Resultat.DEFAVORABLE,
                        LocalDate.of(2025, 8, 1), null));
        ControleTechnique aSupprimer = ControleTechniqueDB.getControlesPourVehicule(vehiculeA).get(0);

        ControleTechniqueDB.supprimerControleTechnique(aSupprimer.getId());

        List<ControleTechnique> restants = ControleTechniqueDB.getControlesPourVehicule(vehiculeA);
        assertEquals(1, restants.size());
        assertNotEquals(aSupprimer.getId(), restants.get(0).getId());
    }

    @Test
    void supprimerUnControleInexistantNeCasseRien() {
        assertDoesNotThrow(() -> ControleTechniqueDB.supprimerControleTechnique(9999));
    }

    @Test
    void supprimerLesControlesDUnVehiculeNeTouchePasAuxAutres() {
        ControleTechniqueDB.ajouterControleTechnique(
                controle(vehiculeA, LocalDate.of(2025, 3, 1), ControleTechnique.Resultat.FAVORABLE,
                        LocalDate.of(2027, 3, 1), null));
        ControleTechniqueDB.ajouterControleTechnique(
                controle(vehiculeB, LocalDate.of(2025, 6, 1), ControleTechnique.Resultat.DEFAVORABLE,
                        LocalDate.of(2025, 8, 1), null));

        ControleTechniqueDB.supprimerControlesPourVehicule(vehiculeA);

        assertTrue(ControleTechniqueDB.getControlesPourVehicule(vehiculeA).isEmpty());
        assertEquals(1, ControleTechniqueDB.getControlesPourVehicule(vehiculeB).size());
    }
}
