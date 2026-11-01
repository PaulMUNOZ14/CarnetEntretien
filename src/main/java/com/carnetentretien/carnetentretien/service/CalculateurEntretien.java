package com.carnetentretien.carnetentretien.service;

import com.carnetentretien.carnetentretien.model.Entretien;
import com.carnetentretien.carnetentretien.model.Preconisation;
import com.carnetentretien.carnetentretien.model.Vehicule;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Calcule, pour un véhicule, l'état de chaque entretien surveillé et sa date d'échéance estimée.
 * Classe sans état : aucun accès à la base ni au réseau.
 */
public class CalculateurEntretien {

    /** Résultat de l'analyse d'un entretien surveillé. */
    public static class Bilan {
        /** Nom de l'entretien (celui de la règle). */
        public String piece;
        /** Usure en % : le plus grand de « km parcourus / intervalle » et « mois écoulés / intervalle ». 100 = échéance atteinte. */
        public double pourcentage;
        /** Libellé d'état avec pictogramme : OK, À PRÉVOIR (≥ 80 %), URGENT (≥ 100 %) ou Action Requise (aucun historique). */
        public String statut;
        /** Texte sur deux lignes : la date prévue (ou de dépassement) et sa cause, puis le dernier entretien connu. */
        public String details;

        public Bilan(String piece, double pourcentage, String statut, String details) {
            this.piece = piece; this.pourcentage = pourcentage; this.statut = statut; this.details = details;
        }
    }

    /**
     * Analyse chaque règle pour un véhicule, dans l'ordre des règles.
     * <p>
     * Pour chaque règle, on retient le dernier entretien du même type (casse ignorée). L'usure est le maximum entre
     * l'usure kilométrique et l'usure en temps. La date d'échéance est la plus proche entre la date estimée
     * d'atteinte de la limite kilométrique (d'après le kilométrage annuel, 10 000 km/an si celui-ci est ≤ 0)
     * et la date limite en mois. Sans historique pour une règle, le bilan est à 0 % avec « Action Requise ».
     *
     * @param vehicule        véhicule analysé (kilométrage actuel et annuel estimé)
     * @param historique      entretiens déjà réalisés pour ce véhicule
     * @param preconisations  règles de suivi
     * @return un {@link Bilan} par règle ; liste vide s'il n'y a aucune règle
     */
    public static List<Bilan> analyserEntretien(Vehicule vehicule, List<Entretien> historique, List<Preconisation> preconisations) {
        List<Bilan> resultats = new ArrayList<>();

        if (preconisations.isEmpty()) {
            return resultats;
        }

        int kmAnnuel = vehicule.getKilometrageAnnuelEstime() > 0 ? vehicule.getKilometrageAnnuelEstime() : 10000;

        for (Preconisation preco : preconisations) {
            Entretien dernierEntretien = historique.stream()
                    .filter(e -> e.getTypeOperation().equalsIgnoreCase(preco.getTypeOperation()))
                    .max((e1, e2) -> e1.getDateRealisation().compareTo(e2.getDateRealisation()))
                    .orElse(null);

            if (dernierEntretien == null) {
                resultats.add(new Bilan(preco.getTypeOperation(), 0, "⚠ Action Requise", "Aucun historique. Renseignez la dernière intervention."));
                continue;
            }

            int kmParcourus = vehicule.getKilometrageActuel() - dernierEntretien.getKilometrageRealisation();
            double pourcentageKm = ((double) kmParcourus / preco.getIntervalleKm()) * 100;

            long moisEcoules = ChronoUnit.MONTHS.between(dernierEntretien.getDateRealisation(), LocalDate.now());
            double pourcentageMois = ((double) moisEcoules / preco.getIntervalleMois()) * 100;

            double usureReelle = Math.max(pourcentageKm, pourcentageMois);

            int limiteKm = dernierEntretien.getKilometrageRealisation() + preco.getIntervalleKm();
            int kmRestants = limiteKm - vehicule.getKilometrageActuel();

            LocalDate datePrevisionnelleKm;
            if (kmRestants > 0) {
                double anneesRestantes = (double) kmRestants / kmAnnuel;
                long joursRestants = (long) (anneesRestantes * 365);
                datePrevisionnelleKm = LocalDate.now().plusDays(joursRestants);
            } else {
                datePrevisionnelleKm = LocalDate.now(); // C'est déjà dépassé
            }

            LocalDate limiteTemps = dernierEntretien.getDateRealisation().plusMonths(preco.getIntervalleMois());

            LocalDate dateEcheanceFinale = datePrevisionnelleKm.isBefore(limiteTemps) ? datePrevisionnelleKm : limiteTemps;

            String causeEcheance = datePrevisionnelleKm.isBefore(limiteTemps) ? "Atteinte des " + limiteKm + " km" : "Délai de " + preco.getIntervalleMois() + " mois écoulé";

            String statut;
            if (usureReelle >= 100) { statut = "🔴 URGENT (Dépassé)"; }
            else if (usureReelle >= 80) { statut = "🟠 À PRÉVOIR"; }
            else { statut = "🟢 OK"; }

            String details;
            if (usureReelle >= 100) {
                details = String.format("Dépassé depuis le %s (Cause : %s)\nDernier entretien le %s à %d km",
                        dateEcheanceFinale, causeEcheance, dernierEntretien.getDateRealisation(), dernierEntretien.getKilometrageRealisation());
            } else {
                details = String.format("À planifier pour le : %s (Cause : %s)\nDernier entretien le %s à %d km",
                        dateEcheanceFinale, causeEcheance, dernierEntretien.getDateRealisation(), dernierEntretien.getKilometrageRealisation());
            }

            resultats.add(new Bilan(preco.getTypeOperation(), usureReelle, statut, details));
        }
        return resultats;
    }
}