package com.carnetentretien.carnetentretien.model;

public class Vehicule {
    private int id;
    private String immatriculation;
    private String marque;
    private String modele;
    private int kilometrageActuel;
    private int kilometrageAnnuelEstime;
    private String imageUrl;   // photo du modèle (récupérée via la plaque), peut être null

    public Vehicule(int id, String immatriculation, String marque, String modele,
                    int kilometrageActuel, int kilometrageAnnuelEstime, String imageUrl) {
        this.id = id;
        this.immatriculation = immatriculation;
        this.marque = marque;
        this.modele = modele;
        this.kilometrageActuel = kilometrageActuel;
        this.kilometrageAnnuelEstime = kilometrageAnnuelEstime;
        this.imageUrl = imageUrl;
    }

    public Vehicule(int id, String immatriculation, String marque, String modele,
                    int kilometrageActuel, int kilometrageAnnuelEstime) {
        this(id, immatriculation, marque, modele, kilometrageActuel, kilometrageAnnuelEstime, null);
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getImmatriculation() { return immatriculation; }
    public void setImmatriculation(String immatriculation) { this.immatriculation = immatriculation; }

    public String getMarque() { return marque; }
    public void setMarque(String marque) { this.marque = marque; }

    public String getModele() { return modele; }
    public void setModele(String modele) { this.modele = modele; }

    public int getKilometrageActuel() { return kilometrageActuel; }
    public void setKilometrageActuel(int kilometrageActuel) { this.kilometrageActuel = kilometrageActuel; }

    public int getKilometrageAnnuelEstime() { return kilometrageAnnuelEstime; }
    public void setKilometrageAnnuelEstime(int kilometrageAnnuelEstime) { this.kilometrageAnnuelEstime = kilometrageAnnuelEstime; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    @Override
    public String toString() {
        return marque + " " + modele + " - " + kilometrageActuel + " km";
    }
}
