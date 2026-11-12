package com.carnetentretien.carnetentretien.model;

import java.time.LocalDate;

/**
 * Un relevé du compteur d'un véhicule, conservé dans l'historique kilométrique.
 * Objet immuable : on le remplace par un nouvel objet pour le modifier.
 */
public class ReleveKilometrage {

    private final int id;
    private final int vehiculeId;

    /** Date à laquelle le compteur a été lu. */
    private final LocalDate dateReleve;

    /** Nombre de kilomètres affiché au compteur ce jour-là. */
    private final int kilometrage;

    /** Remarque libre (trajet, entretien, contrôle…), ou {@code null}. */
    private final String note;

    /**
     * @param id          identifiant en base ({@code 0} avant l'enregistrement)
     * @param vehiculeId  identifiant du véhicule concerné
     * @param dateReleve  date de la lecture du compteur
     * @param kilometrage valeur affichée au compteur
     * @param note        remarque libre, ou {@code null}
     */
    public ReleveKilometrage(int id, int vehiculeId, LocalDate dateReleve, int kilometrage, String note) {
        this.id = id;
        this.vehiculeId = vehiculeId;
        this.dateReleve = dateReleve;
        this.kilometrage = kilometrage;
        this.note = note;
    }

    /** Constructeur sans remarque ({@code note} = {@code null}). */
    public ReleveKilometrage(int id, int vehiculeId, LocalDate dateReleve, int kilometrage) {
        this(id, vehiculeId, dateReleve, kilometrage, null);
    }

    public int getId() { return id; }
    public int getVehiculeId() { return vehiculeId; }
    public LocalDate getDateReleve() { return dateReleve; }
    public int getKilometrage() { return kilometrage; }
    public String getNote() { return note; }

    /** @return « date | 45000 km (remarque) » */
    @Override
    public String toString() {
        String texte = dateReleve + " | " + kilometrage + " km";
        return note == null || note.isBlank() ? texte : texte + " (" + note + ")";
    }
}
