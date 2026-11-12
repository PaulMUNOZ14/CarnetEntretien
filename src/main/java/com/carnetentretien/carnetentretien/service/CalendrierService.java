package com.carnetentretien.carnetentretien.service;

import com.carnetentretien.carnetentretien.db.ControleTechniqueDB;
import com.carnetentretien.carnetentretien.db.ControleTechniquePlanifieDB;
import com.carnetentretien.carnetentretien.db.EntretienDB;
import com.carnetentretien.carnetentretien.db.EntretienPlanifieDB;
import com.carnetentretien.carnetentretien.db.PreconisationDB;
import com.carnetentretien.carnetentretien.db.VehiculeDB;
import com.carnetentretien.carnetentretien.model.ControleTechnique;
import com.carnetentretien.carnetentretien.model.ControleTechniquePlanifie;
import com.carnetentretien.carnetentretien.model.Entretien;
import com.carnetentretien.carnetentretien.model.EntretienPlanifie;
import com.carnetentretien.carnetentretien.model.Preconisation;
import com.carnetentretien.carnetentretien.model.Vehicule;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Rassemble les événements du calendrier : historique, planifications, contrôles techniques
 * et échéances calculées.
 * <p>
 * Une échéance calculée est masquée lorsqu'un entretien du même type est déjà planifié pour le
 * véhicule : la planification remplace alors la prévision. Il en va de même pour le contrôle
 * technique : une planification de contrôle technique remplace l'échéance calculée à partir du
 * dernier contrôle réalisé.
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
        Map<Integer, List<ControleTechnique>> controles = new HashMap<>();
        Map<Integer, List<ControleTechniquePlanifie>> controlesPlanifies = new HashMap<>();
        for (Vehicule v : vehicules) {
            historiques.put(v.getId(), EntretienDB.getEntretiensPourVehicule(v.getId()));
            planifies.put(v.getId(), EntretienPlanifieDB.getEntretiensPlanifiesPourVehicule(v.getId()));
            controles.put(v.getId(), ControleTechniqueDB.getControlesPourVehicule(v.getId()));
            controlesPlanifies.put(v.getId(), ControleTechniquePlanifieDB.getControlesPlanifiesPourVehicule(v.getId()));
        }
        return assembler(vehicules, regles, historiques, planifies, controles, controlesPlanifies);
    }

    /**
     * Construit les événements à partir de données déjà chargées, sans contrôle technique.
     * Utilisée par les tests et par les appelants qui n'ont pas besoin de cette partie.
     */
    public static List<EvenementCalendrier> assembler(List<Vehicule> vehicules, List<Preconisation> regles,
                                                      Map<Integer, List<Entretien>> historiques,
                                                      Map<Integer, List<EntretienPlanifie>> planifies) {
        return assembler(vehicules, regles, historiques, planifies, Map.of(), Map.of());
    }

    /**
     * Construit les événements à partir de données déjà chargées, sans contrôle technique planifié.
     * Utilisée par les tests et par les appelants qui n'ont pas besoin de cette partie.
     */
    public static List<EvenementCalendrier> assembler(List<Vehicule> vehicules, List<Preconisation> regles,
                                                      Map<Integer, List<Entretien>> historiques,
                                                      Map<Integer, List<EntretienPlanifie>> planifies,
                                                      Map<Integer, List<ControleTechnique>> controles) {
        return assembler(vehicules, regles, historiques, planifies, controles, Map.of());
    }

    /**
     * Construit les événements à partir de données déjà chargées (méthode sans accès à la base,
     * utilisée aussi par les tests).
     *
     * @param vehicules          véhicules à traiter
     * @param regles             règles de suivi communes
     * @param historiques        entretiens réalisés, par identifiant de véhicule
     * @param planifies          entretiens planifiés, par identifiant de véhicule
     * @param controles          contrôles techniques réalisés, par identifiant de véhicule
     * @param controlesPlanifies contrôles techniques planifiés, par identifiant de véhicule
     * @return les événements triés par date, puis par type et par véhicule
     */
    public static List<EvenementCalendrier> assembler(List<Vehicule> vehicules, List<Preconisation> regles,
                                                      Map<Integer, List<Entretien>> historiques,
                                                      Map<Integer, List<EntretienPlanifie>> planifies,
                                                      Map<Integer, List<ControleTechnique>> controles,
                                                      Map<Integer, List<ControleTechniquePlanifie>> controlesPlanifies) {
        List<EvenementCalendrier> evenements = new ArrayList<>();

        for (Vehicule v : vehicules) {
            List<Entretien> historique = historiques.getOrDefault(v.getId(), List.of());
            List<EntretienPlanifie> planifie = planifies.getOrDefault(v.getId(), List.of());
            List<ControleTechnique> controle = controles.getOrDefault(v.getId(), List.of());
            List<ControleTechniquePlanifie> controlePlanifie = controlesPlanifies.getOrDefault(v.getId(), List.of());

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

            for (ControleTechnique c : controle) {
                evenements.add(EvenementCalendrier.controleFait(v, c));
            }
            for (ControleTechniquePlanifie p : controlePlanifie) {
                evenements.add(EvenementCalendrier.controlePlanifie(v, p));
            }
            if (controlePlanifie.isEmpty()) {
                ControleTechniqueService.Suivi suivi = ControleTechniqueService.analyser(controle);
                if (suivi.getEtat() != ControleTechniqueService.Etat.AUCUN) {
                    evenements.add(EvenementCalendrier.controle(v, suivi.getDernier(), suivi.isProche()));
                }
            }
        }

        evenements.sort(Comparator.comparing(EvenementCalendrier::getDate)
                .thenComparingInt(e -> e.getType().ordinal())
                .thenComparing(e -> e.getVehicule().getMarque(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(EvenementCalendrier::getOperation, String.CASE_INSENSITIVE_ORDER));
        return evenements;
    }

    /** Compare deux noms d'opération sans tenir compte de la casse, des espaces ni des accents. */
    private static String cle(String operation) {
        if (operation == null) return "";
        String sansAccents = Normalizer.normalize(operation, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sansAccents.trim().toLowerCase(Locale.FRANCE).replaceAll("\\s+", " ");
    }
}
