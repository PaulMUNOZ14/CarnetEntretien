package com.carnetentretien.carnetentretien.service;

import com.carnetentretien.carnetentretien.model.Vehicule;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Rappel destiné à une notification Windows, construit à partir des événements du calendrier.
 * <p>
 * Classe sans état : aucun accès à la base ni au système. Elle sert aussi aux tests de la
 * logique de sélection (quelles échéances méritent une notification, et dans quel ordre).
 */
public final class RappelNotification {

    /** Nombre de jours avant une échéance d'entretien à partir duquel on prévient. */
    public static final int JOURS_AVANT_ECHEANCE = 30;

    /** Nombre de jours avant une planification (entretien ou contrôle) à partir duquel on prévient. */
    public static final int JOURS_AVANT_PLANIFICATION = 7;

    /** Usure minimale (%) à partir de laquelle un entretien est rappelé, même si sa date est lointaine. */
    public static final double USURE_MINIMALE = 80;

    /** Nombre de rappels détaillés dans le texte de la notification. */
    private static final int MAX_DETAILS = 3;

    /** Longueur maximale du texte : certains systèmes tronquent les messages trop longs. */
    private static final int LONGUEUR_MAX_TEXTE = 250;

    private static final DateTimeFormatter DATE_COURTE = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter DATE_AVEC_ANNEE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EvenementCalendrier.Type type;
    private final String operation;
    private final String vehicule;
    private final LocalDate date;
    private final boolean enRetard;
    private final String precision;

    private RappelNotification(EvenementCalendrier.Type type, String operation, String vehicule,
                               LocalDate date, boolean enRetard, String precision) {
        this.type = type;
        this.operation = operation;
        this.vehicule = vehicule;
        this.date = date;
        this.enRetard = enRetard;
        this.precision = precision;
    }

    /**
     * Sélectionne les événements qui méritent une notification :
     * <ul>
     *   <li>échéance d'entretien en retard, dans les 30 jours ou usée à 80 % ou plus ;</li>
     *   <li>contrôle technique en retard ou à prévoir dans les deux mois ;</li>
     *   <li>entretien ou contrôle planifié dans les 7 jours (ou déjà passé).</li>
     * </ul>
     * Les opérations déjà réalisées sont ignorées. Le résultat est trié : retards d'abord,
     * puis par date, véhicule et opération.
     *
     * @param evenements événements du calendrier, dans n'importe quel ordre
     * @param aujourdhui jour de référence (les tests peuvent le fixer)
     * @return les rappels à afficher (liste vide s'il n'y a rien à signaler)
     */
    public static List<RappelNotification> collecter(List<EvenementCalendrier> evenements, LocalDate aujourdhui) {
        Objects.requireNonNull(evenements, "evenements");
        LocalDate reference = aujourdhui != null ? aujourdhui : LocalDate.now();
        List<RappelNotification> rappels = new ArrayList<>();

        for (EvenementCalendrier evenement : evenements) {
            LocalDate date = evenement.getDate();
            if (date == null) continue;

            switch (evenement.getType()) {
                case ECHEANCE -> {
                    if (evenement.isEnRetard()
                            || !date.isAfter(reference.plusDays(JOURS_AVANT_ECHEANCE))
                            || evenement.getPourcentage() >= USURE_MINIMALE) {
                        rappels.add(new RappelNotification(
                                evenement.getType(), evenement.getOperation(), libelle(evenement.getVehicule()),
                                date, evenement.isEnRetard() || date.isBefore(reference),
                                "échéance le " + dateCourte(date, reference)));
                    }
                }
                case CONTROLE -> {
                    if (evenement.isEnRetard() || evenement.isProche()) {
                        rappels.add(new RappelNotification(
                                evenement.getType(), evenement.getOperation(), libelle(evenement.getVehicule()),
                                date, evenement.isEnRetard() || date.isBefore(reference),
                                "contrôle le " + dateCourte(date, reference)));
                    }
                }
                case PLANIFIE -> {
                    if (!date.isAfter(reference.plusDays(JOURS_AVANT_PLANIFICATION))) {
                        rappels.add(new RappelNotification(
                                evenement.getType(), evenement.getOperation(), libelle(evenement.getVehicule()),
                                date, date.isBefore(reference),
                                "prévu le " + dateCourte(date, reference)));
                    }
                }
                case CONTROLE_PLANIFIE -> {
                    if (!date.isAfter(reference.plusDays(JOURS_AVANT_PLANIFICATION))) {
                        rappels.add(new RappelNotification(
                                evenement.getType(), evenement.getOperation(), libelle(evenement.getVehicule()),
                                date, date.isBefore(reference),
                                "contrôle prévu le " + dateCourte(date, reference)));
                    }
                }
                default -> {
                    // Entretien réalisé ou contrôle déjà fait : rien à rappeler.
                }
            }
        }

        rappels.sort(Comparator.comparing((RappelNotification r) -> r.enRetard).reversed()
                .thenComparing(r -> r.date)
                .thenComparing(r -> r.vehicule, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(r -> r.operation, String.CASE_INSENSITIVE_ORDER));
        return rappels;
    }

    /** @return le titre de la notification, par exemple « 2 rappels en retard, 1 à prévoir » */
    public static String titre(List<RappelNotification> rappels) {
        if (rappels == null || rappels.isEmpty()) {
            return "Carnet Entretien";
        }
        long retard = rappels.stream().filter(RappelNotification::isEnRetard).count();
        long aPrevoir = rappels.size() - retard;

        if (retard > 0 && aPrevoir == 0) {
            return retard + (retard > 1 ? " rappels en retard" : " rappel en retard");
        }
        if (retard > 0) {
            return retard + (retard > 1 ? " rappels en retard" : " rappel en retard")
                    + ", " + aPrevoir + " à prévoir";
        }
        return rappels.size() + (rappels.size() > 1 ? " rappels d'entretien" : " rappel d'entretien");
    }

    /** @return le corps de la notification, limité aux trois rappels les plus urgents */
    public static String texte(List<RappelNotification> rappels) {
        if (rappels == null || rappels.isEmpty()) {
            return "Aucune échéance à signaler : tout est à jour.";
        }

        StringBuilder texte = new StringBuilder();
        int detailles = Math.min(MAX_DETAILS, rappels.size());
        for (int i = 0; i < detailles; i++) {
            RappelNotification rappel = rappels.get(i);
            if (i > 0) texte.append('\n');
            texte.append("• ").append(rappel.operation)
                    .append(" — ").append(rappel.vehicule)
                    .append(" : ").append(rappel.precision);
        }

        int restants = rappels.size() - detailles;
        if (restants > 0) {
            texte.append('\n').append("et ").append(restants)
                    .append(restants > 1 ? " autres rappels." : " autre rappel.");
        }
        return tronquer(texte.toString());
    }

    private static String dateCourte(LocalDate date, LocalDate aujourdhui) {
        return date.getYear() == aujourdhui.getYear()
                ? date.format(DATE_COURTE)
                : date.format(DATE_AVEC_ANNEE);
    }

    private static String tronquer(String texte) {
        if (texte.length() <= LONGUEUR_MAX_TEXTE) return texte;
        return texte.substring(0, LONGUEUR_MAX_TEXTE - 1) + "…";
    }

    private static String libelle(Vehicule vehicule) {
        String texte = vehicule.getMarque() + " " + vehicule.getModele();
        if (vehicule.getImmatriculation() != null && !vehicule.getImmatriculation().isBlank()) {
            texte += " (" + vehicule.getImmatriculation() + ")";
        }
        return texte;
    }

    public EvenementCalendrier.Type getType() { return type; }
    public String getOperation() { return operation; }
    public String getVehicule() { return vehicule; }
    public LocalDate getDate() { return date; }
    public boolean isEnRetard() { return enRetard; }
    public String getPrecision() { return precision; }
}
