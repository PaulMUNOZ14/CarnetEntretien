package com.carnetentretien.carnetentretien.model;

import java.time.LocalDate;

/**
 * Un contrôle technique prévu à une date future (ou du jour), saisi par l'utilisateur depuis le
 * calendrier. Il ne s'agit pas d'un contrôle réalisé : une fois le passage effectué, la
 * planification est supprimée et un {@link ControleTechnique} est ajouté à l'historique.
 * Objet immuable : on le remplace par un nouvel objet pour le modifier.
 */
public class ControleTechniquePlanifie {

    private final int id;
    private final int vehiculeId;

    /** Date à laquelle le contrôle est prévu. */
    private final LocalDate datePrevue;

    /** Remarque libre (centre de contrôle, contre-visite à passer…), ou {@code null}. */
    private final String note;

    /**
     * @param id         identifiant en base ({@code 0} avant l'enregistrement)
     * @param vehiculeId identifiant du véhicule concerné
     * @param datePrevue date à laquelle le contrôle est prévu
     * @param note       remarque libre, ou {@code null}
     */
    public ControleTechniquePlanifie(int id, int vehiculeId, LocalDate datePrevue, String note) {
        this.id = id;
        this.vehiculeId = vehiculeId;
        this.datePrevue = datePrevue;
        this.note = note;
    }

    /** Constructeur sans remarque ({@code note} = {@code null}). */
    public ControleTechniquePlanifie(int id, int vehiculeId, LocalDate datePrevue) {
        this(id, vehiculeId, datePrevue, null);
    }

    public int getId() { return id; }
    public int getVehiculeId() { return vehiculeId; }
    public LocalDate getDatePrevue() { return datePrevue; }
    public String getNote() { return note; }

    /** @return « date prévue | Contrôle technique (remarque) » */
    @Override
    public String toString() {
        return datePrevue + " | Contrôle technique" + (note != null && !note.isBlank() ? " (" + note + ")" : "");
    }
}
