package com.carnetentretien.carnetentretien.service;

import com.carnetentretien.carnetentretien.model.Entretien;
import com.carnetentretien.carnetentretien.model.EntretienPlanifie;
import com.carnetentretien.carnetentretien.model.Vehicule;

import java.time.LocalDate;

/**
 * Un élément affiché dans le calendrier : un entretien déjà réalisé, un entretien planifié
 * par l'utilisateur, ou une échéance estimée par {@link CalculateurEntretien}.
 * Objet immuable.
 */
public class EvenementCalendrier {

    /** Origine de l'événement, dans l'ordre d'affichage à une même date. */
    public enum Type { ECHEANCE, PLANIFIE, REALISE }

    private final Type type;
    private final LocalDate date;
    private final Vehicule vehicule;
    private final String operation;

    /** Usure en % (uniquement pour une échéance, {@code 0} sinon). */
    private final double pourcentage;

    /** {@code true} pour une échéance déjà dépassée. */
    private final boolean enRetard;

    private final Entretien entretien;
    private final EntretienPlanifie planifie;

    private EvenementCalendrier(Type type, LocalDate date, Vehicule vehicule, String operation,
                                double pourcentage, boolean enRetard,
                                Entretien entretien, EntretienPlanifie planifie) {
        this.type = type;
        this.date = date;
        this.vehicule = vehicule;
        this.operation = operation;
        this.pourcentage = pourcentage;
        this.enRetard = enRetard;
        this.entretien = entretien;
        this.planifie = planifie;
    }

    /** Échéance calculée à partir de la dernière intervention et de la règle de suivi. */
    public static EvenementCalendrier echeance(Vehicule vehicule, CalculateurEntretien.Bilan bilan) {
        return new EvenementCalendrier(Type.ECHEANCE, bilan.dateEcheance, vehicule, bilan.piece,
                bilan.pourcentage, bilan.depasse, null, null);
    }

    /** Entretien déjà réalisé (ligne de l'historique). */
    public static EvenementCalendrier realise(Vehicule vehicule, Entretien entretien) {
        return new EvenementCalendrier(Type.REALISE, entretien.getDateRealisation(), vehicule,
                entretien.getTypeOperation(), 0, false, entretien, null);
    }

    /** Entretien planifié par l'utilisateur depuis le calendrier. */
    public static EvenementCalendrier planifie(Vehicule vehicule, EntretienPlanifie planifie) {
        return new EvenementCalendrier(Type.PLANIFIE, planifie.getDatePrevue(), vehicule,
                planifie.getTypeOperation(), 0, false, null, planifie);
    }

    public Type getType() { return type; }
    public LocalDate getDate() { return date; }
    public Vehicule getVehicule() { return vehicule; }
    public String getOperation() { return operation; }
    public double getPourcentage() { return pourcentage; }
    public boolean isEnRetard() { return enRetard; }
    public Entretien getEntretien() { return entretien; }
    public EntretienPlanifie getPlanifie() { return planifie; }
}
