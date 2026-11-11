package com.carnetentretien.carnetentretien.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Un contrôle technique réalisé pour un véhicule.
 * <p>
 * Le résultat détermine la date du prochain contrôle : deux ans plus tard si le contrôle est
 * favorable, deux mois plus tard s'il est défavorable (contre-visite). Cette date est calculée
 * à la saisie mais reste modifiable, car le rapport officiel fait foi.
 * Objet immuable : on le remplace par un nouvel objet pour le modifier.
 */
public class ControleTechnique {

    /** Résultat d'un contrôle technique, avec le délai réglementaire avant le suivant. */
    public enum Resultat {
        FAVORABLE("Favorable", 2, ChronoUnit.YEARS),
        DEFAVORABLE("Défavorable", 2, ChronoUnit.MONTHS);

        private final String libelle;
        private final int quantite;
        private final ChronoUnit unite;

        Resultat(String libelle, int quantite, ChronoUnit unite) {
            this.libelle = libelle;
            this.quantite = quantite;
            this.unite = unite;
        }

        public String getLibelle() { return libelle; }

        /** @return la date proposée pour le prochain contrôle après un contrôle réalisé le jour donné */
        public LocalDate dateProchaine(LocalDate dateRealisation) {
            return dateRealisation.plus(quantite, unite);
        }

        /** @return le libellé français, utilisé notamment dans les listes déroulantes */
        @Override
        public String toString() { return libelle; }
    }

    private final int id;
    private final int vehiculeId;
    private final LocalDate dateRealisation;
    private final Resultat resultat;
    private final LocalDate dateProchaine;

    /** Remarque libre (défauts mineurs, garage…), ou {@code null}. */
    private final String note;

    /**
     * @param id              identifiant en base ({@code 0} avant l'enregistrement)
     * @param vehiculeId      identifiant du véhicule concerné
     * @param dateRealisation date du contrôle
     * @param resultat        résultat du contrôle
     * @param dateProchaine   date du prochain contrôle indiquée sur le rapport
     * @param note            remarque libre, ou {@code null}
     */
    public ControleTechnique(int id, int vehiculeId, LocalDate dateRealisation, Resultat resultat,
                             LocalDate dateProchaine, String note) {
        this.id = id;
        this.vehiculeId = vehiculeId;
        this.dateRealisation = dateRealisation;
        this.resultat = resultat;
        this.dateProchaine = dateProchaine;
        this.note = note;
    }

    /** Constructeur sans remarque ({@code note} = {@code null}). */
    public ControleTechnique(int id, int vehiculeId, LocalDate dateRealisation, Resultat resultat,
                             LocalDate dateProchaine) {
        this(id, vehiculeId, dateRealisation, resultat, dateProchaine, null);
    }

    public int getId() { return id; }
    public int getVehiculeId() { return vehiculeId; }
    public LocalDate getDateRealisation() { return dateRealisation; }
    public Resultat getResultat() { return resultat; }
    public LocalDate getDateProchaine() { return dateProchaine; }
    public String getNote() { return note; }

    /** @return « date | résultat | prochain contrôle le … » */
    @Override
    public String toString() {
        return dateRealisation + " | " + resultat.getLibelle() + " | prochain contrôle le " + dateProchaine;
    }
}
