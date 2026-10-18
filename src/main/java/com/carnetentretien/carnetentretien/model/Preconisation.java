package com.carnetentretien.carnetentretien.model;

public class Preconisation {
    private int id;
    private String typeOperation;
    private int intervalleKm;
    private int intervalleMois;

    public Preconisation(int id, String typeOperation, int intervalleKm, int intervalleMois) {
        this.id = id;
        this.typeOperation = typeOperation;
        this.intervalleKm = intervalleKm;
        this.intervalleMois = intervalleMois;
    }

    public int getId() { return id; }
    public String getTypeOperation() { return typeOperation; }
    public int getIntervalleKm() { return intervalleKm; }
    public int getIntervalleMois() { return intervalleMois; }

    @Override
    public String toString() {
        return typeOperation + " (Tous les " + intervalleKm + " km ou " + intervalleMois + " mois)";
    }
}