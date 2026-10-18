package com.carnetentretien.carnetentretien.service;

import com.carnetentretien.carnetentretien.model.Entretien;
import com.carnetentretien.carnetentretien.model.Preconisation;
import com.carnetentretien.carnetentretien.model.Vehicule;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class CalculateurEntretien {

    public static class Bilan {
        public String piece;
        public double pourcentage;
        public String statut;
        public String details;

        public Bilan(String piece, double pourcentage, String statut, String details) {
            this.piece = piece; this.pourcentage = pourcentage; this.statut = statut; this.details = details;
        }
    }

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