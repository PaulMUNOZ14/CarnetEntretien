package com.carnetentretien.carnetentretien.service;

import com.carnetentretien.carnetentretien.model.ReleveKilometrage;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HistoriqueKilometrageServiceTest {

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 6, 1);

    private final ReleveKilometrage mars = new ReleveKilometrage(
            1, 7, LocalDate.of(2026, 3, 1), 50000, null);
    private final ReleveKilometrage avril = new ReleveKilometrage(
            2, 7, LocalDate.of(2026, 4, 1), 55000, null);

    @Test
    void unReleveEntreDeuxDatesEstAccepte() {
        ReleveKilometrage candidat = new ReleveKilometrage(
                0, 7, LocalDate.of(2026, 3, 15), 52000, null);

        assertNull(HistoriqueKilometrageService.verifier(List.of(mars, avril), candidat, AUJOURDHUI));
    }

    @Test
    void uneDateFutureEstRefusee() {
        ReleveKilometrage candidat = new ReleveKilometrage(
                0, 7, LocalDate.of(2026, 7, 1), 60000, null);

        String erreur = HistoriqueKilometrageService.verifier(List.of(mars, avril), candidat, AUJOURDHUI);

        assertNotNull(erreur);
        assertTrue(erreur.contains("futur"));
    }

    @Test
    void uneDateManquanteEstRefusee() {
        ReleveKilometrage candidat = new ReleveKilometrage(0, 7, null, 60000, null);

        assertNotNull(HistoriqueKilometrageService.verifier(List.of(), candidat, AUJOURDHUI));
    }

    @Test
    void unKilometrageNegatifEstRefuse() {
        ReleveKilometrage candidat = new ReleveKilometrage(
                0, 7, LocalDate.of(2026, 5, 1), -1, null);

        String erreur = HistoriqueKilometrageService.verifier(List.of(), candidat, AUJOURDHUI);

        assertNotNull(erreur);
        assertTrue(erreur.contains("négatif"));
    }

    @Test
    void unReleveSousLePrecedentEstRefuse() {
        ReleveKilometrage candidat = new ReleveKilometrage(
                0, 7, LocalDate.of(2026, 3, 15), 49000, null);

        String erreur = HistoriqueKilometrageService.verifier(List.of(mars, avril), candidat, AUJOURDHUI);

        assertNotNull(erreur);
        assertTrue(erreur.contains("reculer"));
    }

    @Test
    void unReleveAuDessusDuSuivantEstRefuse() {
        ReleveKilometrage candidat = new ReleveKilometrage(
                0, 7, LocalDate.of(2026, 3, 15), 56000, null);

        String erreur = HistoriqueKilometrageService.verifier(List.of(mars, avril), candidat, AUJOURDHUI);

        assertNotNull(erreur);
        assertTrue(erreur.contains("dépasser"));
    }

    @Test
    void unReleveIdentiqueEstRefuse() {
        ReleveKilometrage candidat = new ReleveKilometrage(
                0, 7, LocalDate.of(2026, 3, 1), 50000, null);

        String erreur = HistoriqueKilometrageService.verifier(List.of(mars, avril), candidat, AUJOURDHUI);

        assertNotNull(erreur);
        assertTrue(erreur.contains("identique"));
    }

    @Test
    void leReleveEnCoursDEditionNEstPasCompareALuiMeme() {
        ReleveKilometrage modifie = new ReleveKilometrage(
                avril.getId(), 7, avril.getDateReleve(), 54000, "Correction");

        assertNull(HistoriqueKilometrageService.verifier(List.of(mars, avril), modifie, AUJOURDHUI));
    }

    @Test
    void unNouveauReleveDuMemeJourDoitEtreSuperieurOuEgalAuDernier() {
        ReleveKilometrage premier = new ReleveKilometrage(
                1, 7, LocalDate.of(2026, 5, 1), 60000, null);

        ReleveKilometrage accepte = new ReleveKilometrage(
                0, 7, LocalDate.of(2026, 5, 1), 61000, null);
        assertNull(HistoriqueKilometrageService.verifier(List.of(premier), accepte, AUJOURDHUI));

        ReleveKilometrage refuse = new ReleveKilometrage(
                0, 7, LocalDate.of(2026, 5, 1), 59000, null);
        assertNotNull(HistoriqueKilometrageService.verifier(List.of(premier), refuse, AUJOURDHUI));
    }

    @Test
    void unReleveAnterieurPeutEtreAjouteSIlResteSousLeSuivant() {
        ReleveKilometrage candidat = new ReleveKilometrage(
                0, 7, LocalDate.of(2026, 2, 1), 48000, null);

        assertNull(HistoriqueKilometrageService.verifier(List.of(mars, avril), candidat, AUJOURDHUI));
    }
}
