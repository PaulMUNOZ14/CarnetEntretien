package com.carnetentretien.carnetentretien.service;

import com.carnetentretien.carnetentretien.db.EntretienDB;
import com.carnetentretien.carnetentretien.db.EntretienPlanifieDB;
import com.carnetentretien.carnetentretien.db.PreconisationDB;
import com.carnetentretien.carnetentretien.db.VehiculeDB;
import com.carnetentretien.carnetentretien.model.Entretien;
import com.carnetentretien.carnetentretien.model.EntretienPlanifie;
import com.carnetentretien.carnetentretien.model.Preconisation;
import com.carnetentretien.carnetentretien.model.Vehicule;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Rassemble les événements du calendrier : historique, planifications et échéances calculées.
 * <p>
 * Une échéance calculée est masquée lorsqu'un entretien du même type est déjà planifié pour le
 * véhicule : la planification remplace alors la prévision.
 */
public final class CalendrierService {

    private CalendrierService() {
    }

    /**
     * Charge les événements depuis la base.
     *
     * @param filtre véhicule à afficher, ou {@code null} pour tous les véhicules
     * @return les événements triés par date, puis par type et par véhicule
     */
    public static List<EvenementCalendrier> charger(Vehicule filtre) {
        List<Vehicule> vehicules = filtre != null ? List.of(filtre) : VehiculeDB.getTousLesVehicules();
        List<Preconisation> regles = PreconisationDB.getToutesLesPreconisations();

        Map<Integer, List<Entretien>> historiques = new HashMap<>();
        Map<Integer, List<EntretienPlanifie>> planifies = new HashMap<>();
        for (Vehicule v : vehicules) {
            historiques.put(v.getId(), EntretienDB.getEntretiensPourVehicule(v.getId()));
            planifies.put(v.getId(), EntretienPlanifieDB.getEntretiensPlanifiesPourVehicule(v.getId()));
        }
        return assembler(vehicules, regles, historiques, planifies);
    }

    /**
     * Construit les événements à partir de données déjà chargées (méthode sans accès à la base,
     * utilisée aussi par les tests).
     *
     * @param vehicules   véhicules à traiter
     * @param regles      règles de suivi communes
     * @param historiques entretiens réalisés, par identifiant de véhicule
     * @param planifies   entretiens planifiés, par identifiant de véhicule
     * @return les événements triés par date, puis par type et par véhicule
     */
    public static List<EvenementCalendrier> assembler(List<Vehicule> vehicules, List<Preconisation> regles,
                                                      Map<Integer, List<Entretien>> historiques,
                                                      Map<Integer, List<EntretienPlanifie>> planifies) {
        List<EvenementCalendrier> evenements = new ArrayList<>();

        for (Vehicule v : vehicules) {
            List<Entretien> historique = historiques.getOrDefault(v.getId(), List.of());
            List<EntretienPlanifie> planifie = planifies.getOrDefault(v.getId(), List.of());

            Set<String> operationsPlanifiees = new HashSet<>();
            for (EntretienPlanifie p : planifie) {
                operationsPlanifiees.add(cle(p.getTypeOperation()));
                evenements.add(EvenementCalendrier.planifie(v, p));
            }
            for (Entretien e : historique) {
                evenements.add(EvenementCalendrier.realise(v, e));
            }
            for (CalculateurEntretien.Bilan bilan : CalculateurEntretien.analyserEntretien(v, historique, regles)) {
                if (bilan.dateEcheance != null && !operationsPlanifiees.contains(cle(bilan.piece))) {
                    evenements.add(EvenementCalendrier.echeance(v, bilan));
                }
            }
        }

        evenements.sort(Comparator.comparing(EvenementCalendrier::getDate)
                .thenComparingInt(e -> e.getType().ordinal())
                .thenComparing(e -> e.getVehicule().getMarque(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(EvenementCalendrier::getOperation, String.CASE_INSENSITIVE_ORDER));
        return evenements;
    }

    private static String cle(String operation) {
        return operation == null ? "" : operation.trim().toLowerCase(Locale.FRANCE);
    }
}
