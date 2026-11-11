package com.carnetentretien.carnetentretien.model;

import java.time.LocalDate;

/**
 * Un entretien prévu à une date future (ou du jour), saisi par l'utilisateur depuis le calendrier.
 * Il ne s'agit pas d'un entretien réalisé : une fois l'intervention faite, la planification est
 * supprimée et un {@link Entretien} est ajouté à l'historique du véhicule.
 * Objet immuable : on le remplace par un nouvel objet pour le modifier.
 */
public class EntretienPlanifie {
    private int id;
    private int vehiculeId;

    /** Type d'opération, par exemple « Vidange ». */
    private String typeOperation;
    private LocalDate datePrevue;

    /** Remarque libre (garage, pièces à commander…), ou {@code null}. */
    private String note;

    /**
     * @param id            identifiant en base ({@code 0} avant l'enregistrement)
     * @param vehiculeId    identifiant du véhicule concerné
     * @param typeOperation nature de l'entretien prévu
     * @param datePrevue    date à laquelle il est prévu
     * @param note          remarque libre, ou {@code null}
     */
    public EntretienPlanifie(int id, int vehiculeId, String typeOperation, LocalDate datePrevue, String note) {
        this.id = id;
        this.vehiculeId = vehiculeId;
        this.typeOperation = typeOperation;
        this.datePrevue = datePrevue;
        this.note = note;
    }

    /** Constructeur sans remarque ({@code note} = {@code null}). */
    public EntretienPlanifie(int id, int vehiculeId, String typeOperation, LocalDate datePrevue) {
        this(id, vehiculeId, typeOperation, datePrevue, null);
    }

    // Getters
    public int getId() { return id; }
    public int getVehiculeId() { return vehiculeId; }
    public String getTypeOperation() { return typeOperation; }
    public LocalDate getDatePrevue() { return datePrevue; }
    public String getNote() { return note; }

    /** @return « date prévue | opération (remarque) » */
    @Override
    public String toString() {
        return datePrevue + " | " + typeOperation + (note != null && !note.isBlank() ? " (" + note + ")" : "");
    }
}
