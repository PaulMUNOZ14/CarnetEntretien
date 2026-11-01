package com.carnetentretien.carnetentretien.model;

import java.time.LocalDate;

/**
 * Un entretien déjà réalisé (une ligne de l'historique d'un véhicule).
 * Objet immuable : on le remplace par un nouvel objet pour le modifier.
 */
public class Entretien {
    private int id;
    private int vehiculeId;

    /** Type d'opération, par exemple « Vidange ». Comparé sans tenir compte de la casse avec les {@link Preconisation}. */
    private String typeOperation;
    private LocalDate dateRealisation;

    /** Kilométrage du véhicule le jour de l'opération. */
    private int kilometrageRealisation;

    /** Coût en euros ({@code 0} si inconnu). */
    private double cout;

    /**
     * @param id                     identifiant en base ({@code 0} avant l'enregistrement)
     * @param vehiculeId             identifiant du véhicule concerné
     * @param typeOperation          nature de l'entretien
     * @param dateRealisation        date à laquelle il a été fait
     * @param kilometrageRealisation kilométrage ce jour-là
     * @param cout                   prix payé, en euros
     */
    public Entretien(int id, int vehiculeId, String typeOperation, LocalDate dateRealisation, int kilometrageRealisation, double cout) {
        this.id = id;
        this.vehiculeId = vehiculeId;
        this.typeOperation = typeOperation;
        this.dateRealisation = dateRealisation;
        this.kilometrageRealisation = kilometrageRealisation;
        this.cout = cout;
    }

    // Getters
    public int getId() { return id; }
    public int getVehiculeId() { return vehiculeId; }
    public String getTypeOperation() { return typeOperation; }
    public LocalDate getDateRealisation() { return dateRealisation; }
    public int getKilometrageRealisation() { return kilometrageRealisation; }
    public double getCout() { return cout; }

    /** @return « date | opération à X km (prix €) » */
    @Override
    public String toString() {
        return dateRealisation + " | " + typeOperation + " à " + kilometrageRealisation + " km (" + cout + " €)";
    }
}