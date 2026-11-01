package com.carnetentretien.carnetentretien.model;

/**
 * Règle de suivi d'un entretien : à renouveler tous les X km <b>ou</b> tous les Y mois
 * (le premier des deux atteint déclenche l'alerte). Objet immuable.
 */
public class Preconisation {
    private int id;
    private String typeOperation;
    private int intervalleKm;
    private int intervalleMois;

    /**
     * @param id             identifiant en base ({@code 0} avant l'enregistrement)
     * @param typeOperation  nom de l'entretien surveillé (ex. « Vidange »)
     * @param intervalleKm   intervalle en kilomètres (&gt; 0)
     * @param intervalleMois intervalle en mois (&gt; 0)
     */
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

    /** @return par exemple « Vidange (Tous les 15000 km ou 12 mois) » */
    @Override
    public String toString() {
        return typeOperation + " (Tous les " + intervalleKm + " km ou " + intervalleMois + " mois)";
    }
}