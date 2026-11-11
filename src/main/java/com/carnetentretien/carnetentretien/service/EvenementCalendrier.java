package com.carnetentretien.carnetentretien.service;

import com.carnetentretien.carnetentretien.model.ControleTechnique;
import com.carnetentretien.carnetentretien.model.Entretien;
import com.carnetentretien.carnetentretien.model.EntretienPlanifie;
import com.carnetentretien.carnetentretien.model.Vehicule;

import java.time.LocalDate;

/**
 * Un élément affiché dans le calendrier : un entretien déjà réalisé, un entretien planifié,
 * une échéance estimée par {@link CalculateurEntretien} ou un contrôle technique.
 * Objet immuable.
 */
public class EvenementCalendrier {

    /** Origine de l'événement, dans l'ordre d'affichage à une même date. */
    public enum Type { ECHEANCE, CONTROLE, PLANIFIE, REALISE, CONTROLE_FAIT }

    private final Type type;
    private final LocalDate date;
    private final Vehicule vehicule;
    private final String operation;

    /** Usure en % (uniquement pour une échéance, {@code 0} sinon). */
    private final double pourcentage;

    /** {@code true} pour une échéance déjà dépassée. */
    private final boolean enRetard;

    /** {@code true} pour un contrôle technique à prévoir dans les deux mois. */
    private final boolean proche;

    private final Entretien entretien;
    private final EntretienPlanifie planifie;
    private final ControleTechnique controle;

    private EvenementCalendrier(Type type, LocalDate date, Vehicule vehicule, String operation, double pourcentage, boolean enRetard, boolean proche, Entretien entretien, EntretienPlanifie planifie, ControleTechnique controle) {
        this.type = type;
        this.date = date;
        this.vehicule = vehicule;
        this.operation = operation;
        this.pourcentage = pourcentage;
        this.enRetard = enRetard;
        this.proche = proche;
        this.entretien = entretien;
        this.planifie = planifie;
        this.controle = controle;
    }

    /** Échéance calculée à partir de la dernière intervention et de la règle de suivi. */
    public static EvenementCalendrier echeance(Vehicule vehicule, CalculateurEntretien.Bilan bilan) {
        return new EvenementCalendrier(Type.ECHEANCE, bilan.dateEcheance, vehicule, bilan.piece, bilan.pourcentage, bilan.depasse, false, null, null, null);
    }

    /** Entretien déjà réalisé (ligne de l'historique). */
    public static EvenementCalendrier realise(Vehicule vehicule, Entretien entretien) {
        return new EvenementCalendrier(Type.REALISE, entretien.getDateRealisation(), vehicule, entretien.getTypeOperation(), 0, false, false, entretien, null, null);
    }

    /** Entretien planifié par l'utilisateur depuis le calendrier. */
    public static EvenementCalendrier planifie(Vehicule vehicule, EntretienPlanifie planifie) {
        return new EvenementCalendrier(Type.PLANIFIE, planifie.getDatePrevue(), vehicule, planifie.getTypeOperation(), 0, false, false, null, planifie, null);
    }

    /**
     * Prochaine échéance du contrôle technique d'un véhicule.
     *
     * @param proche {@code true} si l'échéance est dans les deux mois ou déjà dépassée
     */
    public static EvenementCalendrier controle(Vehicule vehicule, ControleTechnique controle, boolean proche) {
        return new EvenementCalendrier(Type.CONTROLE, controle.getDateProchaine(), vehicule, ControleTechniqueService.OPERATION, 0, controle.getDateProchaine().isBefore(LocalDate.now()), proche, null, null, controle);
    }

    /** Contrôle technique déjà réalisé (ligne de l'historique). */
    public static EvenementCalendrier controleFait(Vehicule vehicule, ControleTechnique controle) {
        return new EvenementCalendrier(Type.CONTROLE_FAIT, controle.getDateRealisation(), vehicule, ControleTechniqueService.OPERATION, 0, false, false, null, null, controle);
    }

    public Type getType() { return type; }
    public LocalDate getDate() { return date; }
    public Vehicule getVehicule() { return vehicule; }
    public String getOperation() { return operation; }
    public double getPourcentage() { return pourcentage; }
    public boolean isEnRetard() { return enRetard; }
    public boolean isProche() { return proche; }
    public Entretien getEntretien() { return entretien; }
    public EntretienPlanifie getPlanifie() { return planifie; }
    public ControleTechnique getControle() { return controle; }
}
