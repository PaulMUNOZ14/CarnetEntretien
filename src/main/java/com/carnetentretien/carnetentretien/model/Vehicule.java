package com.carnetentretien.carnetentretien.model;

public class Vehicule {
    private int id;
    private String marque;
    private String modele;
    private int kilometrageActuel;
    private int kilometrageAnnuelEstime;

    public Vehicule(int id, String marque, String modele, int kilometrageActuel, int kilometrageAnnuelEstime) {
        this.id = id;
        this.marque = marque;
        this.modele = modele;
        this.kilometrageActuel = kilometrageActuel;
        this.kilometrageAnnuelEstime = kilometrageAnnuelEstime;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getMarque() { return marque; }
    public void setMarque(String marque) { this.marque = marque; }

    public String getModele() { return modele; }
    public void setModele(String modele) { this.modele = modele; }

    public int getKilometrageActuel() { return kilometrageActuel; }
    public void setKilometrageActuel(int kilometrageActuel) { this.kilometrageActuel = kilometrageActuel; }

    public int getKilometrageAnnuelEstime() { return kilometrageAnnuelEstime; }
    public void setKilometrageAnnuelEstime(int kilometrageAnnuelEstime) { this.kilometrageAnnuelEstime = kilometrageAnnuelEstime; }
}