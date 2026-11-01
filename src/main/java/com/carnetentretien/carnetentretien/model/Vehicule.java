package com.carnetentretien.carnetentretien.model;

/**
 * Un véhicule suivi dans l'application.
 * <p>
 * Les accesseurs ({@code getX}/{@code setX}) donnent accès aux champs du même nom sans traitement particulier.
 */
public class Vehicule {
    private int id;

    /** Plaque d'immatriculation (formatée {@code AB-123-CD}) ; {@code null} si l'utilisateur ne l'a pas renseignée. */
    private String immatriculation;
    private String marque;
    private String modele;

    /** Dernier kilométrage relevé au compteur. */
    private int kilometrageActuel;

    /** Kilométrage annuel estimé, utilisé pour prévoir les dates d'échéance. */
    private int kilometrageAnnuelEstime;

    /** Adresse web de la photo du modèle, trouvée via la plaque ; {@code null} s'il n'y en a pas. */
    private String imageUrl;

    /** Nom (pas le chemin) de la photo choisie par l'utilisateur, copiée dans {@code images/} ; {@code null} = aucune. */
    private String imageLocale;

    /** Constructeur complet. @param id identifiant en base ({@code 0} avant l'enregistrement) */
    public Vehicule(int id, String immatriculation, String marque, String modele,
                    int kilometrageActuel, int kilometrageAnnuelEstime, String imageUrl, String imageLocale) {
        this.id = id;
        this.immatriculation = immatriculation;
        this.marque = marque;
        this.modele = modele;
        this.kilometrageActuel = kilometrageActuel;
        this.kilometrageAnnuelEstime = kilometrageAnnuelEstime;
        this.imageUrl = imageUrl;
        this.imageLocale = imageLocale;
    }

    /** Constructeur sans photo personnelle ({@code imageLocale} = {@code null}). */
    public Vehicule(int id, String immatriculation, String marque, String modele,
                    int kilometrageActuel, int kilometrageAnnuelEstime, String imageUrl) {
        this(id, immatriculation, marque, modele, kilometrageActuel, kilometrageAnnuelEstime, imageUrl, null);
    }

    /** Constructeur sans aucune photo ({@code imageUrl} et {@code imageLocale} = {@code null}). */
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

    public String getImageLocale() { return imageLocale; }
    public void setImageLocale(String imageLocale) { this.imageLocale = imageLocale; }

    /** @return une description courte, par exemple « Peugeot 208 - 45000 km » */
    @Override
    public String toString() {
        return marque + " " + modele + " - " + kilometrageActuel + " km";
    }
}
