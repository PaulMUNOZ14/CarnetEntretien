package com.carnetentretien.carnetentretien.service;

import com.carnetentretien.carnetentretien.model.ControleTechnique;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

/**
 * Analyse l'historique des contrôles techniques d'un véhicule : dernier contrôle connu,
 * date du prochain contrôle et niveau d'alerte.
 * <p>
 * Classe sans état : aucun accès à la base ni au réseau.
 */
public final class ControleTechniqueService {

    /** Nom de l'opération utilisé dans le calendrier. */
    public static final String OPERATION = "Contrôle technique";

    /** Nombre de mois avant l'échéance à partir duquel le contrôle est signalé. */
    public static final int MOIS_ALERTE = 2;

    /** Niveau d'alerte du prochain contrôle. */
    public enum Etat {
        AUCUN("Aucun contrôle enregistré"),
        OK("À jour"),
        A_PREVOIR("À prévoir"),
        EN_RETARD("En retard");

        private final String libelle;

        Etat(String libelle) { this.libelle = libelle; }

        public String getLibelle() { return libelle; }

        /** @return le suffixe utilisé par les classes CSS (par exemple {@code ok}, {@code prevoir}, {@code urgent}) */
        public String suffixe() {
            return switch (this) {
                case OK -> "ok";
                case A_PREVOIR -> "prevoir";
                case EN_RETARD -> "urgent";
                case AUCUN -> "neutre";
            };
        }
    }

    /** Résultat de l'analyse : dernier contrôle, échéance et état. */
    public static class Suivi {
        private final ControleTechnique dernier;
        private final LocalDate dateProchaine;
        private final long joursRestants;
        private final Etat etat;

        private Suivi(ControleTechnique dernier, LocalDate dateProchaine, long joursRestants, Etat etat) {
            this.dernier = dernier;
            this.dateProchaine = dateProchaine;
            this.joursRestants = joursRestants;
            this.etat = etat;
        }

        /** @return le contrôle le plus récent, ou {@code null} si l'historique est vide */
        public ControleTechnique getDernier() { return dernier; }
        /** @return la date du prochain contrôle, ou {@code null} si l'historique est vide */
        public LocalDate getDateProchaine() { return dateProchaine; }
        /** @return nombre de jours avant l'échéance (négatif si elle est dépassée) */
        public long getJoursRestants() { return joursRestants; }
        public Etat getEtat() { return etat; }

        /** {@code true} si le contrôle est à prévoir ou déjà en retard. */
        public boolean isProche() { return etat == Etat.A_PREVOIR || etat == Etat.EN_RETARD; }
    }

    private ControleTechniqueService() {
    }

    /** @return la date proposée pour le prochain contrôle (2 ans si favorable, 2 mois sinon) */
    public static LocalDate prochaineDate(LocalDate dateRealisation, ControleTechnique.Resultat resultat) {
        return resultat.dateProchaine(dateRealisation);
    }

    /** @return le contrôle le plus récent (date de réalisation, puis identifiant), ou {@code null} */
    public static ControleTechnique dernier(List<ControleTechnique> controles) {
        return controles.stream()
                .max(Comparator.comparing(ControleTechnique::getDateRealisation)
                        .thenComparingInt(ControleTechnique::getId))
                .orElse(null);
    }

    /**
     * Analyse un historique de contrôles. Seul le contrôle le plus récent détermine l'échéance :
     * les contrôles plus anciens restent dans l'historique mais leur date de prochaine visite
     * est remplacée par celle du contrôle suivant.
     *
     * @param controles contrôles d'un véhicule, dans n'importe quel ordre
     * @return l'état du prochain contrôle (état {@link Etat#AUCUN} si la liste est vide)
     */
    public static Suivi analyser(List<ControleTechnique> controles) {
        ControleTechnique dernier = dernier(controles);
        if (dernier == null) {
            return new Suivi(null, null, 0, Etat.AUCUN);
        }

        LocalDate aujourdhui = LocalDate.now();
        LocalDate prochaine = dernier.getDateProchaine();
        long jours = ChronoUnit.DAYS.between(aujourdhui, prochaine);

        Etat etat;
        if (prochaine.isBefore(aujourdhui)) {
            etat = Etat.EN_RETARD;
        } else if (!prochaine.isAfter(aujourdhui.plusMonths(MOIS_ALERTE))) {
            etat = Etat.A_PREVOIR;
        } else {
            etat = Etat.OK;
        }
        return new Suivi(dernier, prochaine, jours, etat);
    }
}
