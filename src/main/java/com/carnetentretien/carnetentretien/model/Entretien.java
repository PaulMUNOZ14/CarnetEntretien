package com.carnetentretien.carnetentretien.model;

import java.time.LocalDate;

public class Entretien {
    private int id;
    private int vehiculeId;
    private String typeOperation; // Ex: "Vidange", "Courroie de distribution"
    private LocalDate dateRealisation;
    private int kilometrageRealisation;
    private double cout;

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

    @Override
    public String toString() {
        return dateRealisation + " | " + typeOperation + " à " + kilometrageRealisation + " km (" + cout + " €)";
    }
}