package com.carnetentretien.carnetentretien.service;

import com.carnetentretien.carnetentretien.model.ReleveKilometrage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;

/**
 * Vérifie la cohérence d'un relevé kilométrique avant son enregistrement : le compteur ne peut
 * ni reculer par rapport au relevé précédent, ni dépasser le relevé suivant.
 */
public final class HistoriqueKilometrageService {

    private static final DateTimeFormatter DATE_LONGUE =
            DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Locale.FRANCE);

    private HistoriqueKilometrageService() {
    }

    /**
     * Vérifie qu'un relevé peut être ajouté ou modifié sans rendre l'historique incohérent.
     *
     * @param existants relevés déjà enregistrés pour le véhicule
     * @param candidat  relevé à enregistrer ({@code id = 0} pour une création)
     * @param aujourdhui date du jour, utilisée pour refuser une date future
     * @return {@code null} si le relevé est valide, sinon le message d'erreur à afficher
     */
    public static String verifier(List<ReleveKilometrage> existants, ReleveKilometrage candidat,
                                  LocalDate aujourdhui) {
        if (candidat.getDateReleve() == null) {
            return "Veuillez indiquer la date du relevé.";
        }
        if (candidat.getDateReleve().isAfter(aujourdhui)) {
            return "La date du relevé ne peut pas être dans le futur.";
        }
        if (candidat.getKilometrage() < 0) {
            return "Le kilométrage ne peut pas être négatif.";
        }

        ReleveKilometrage precedent = null;
        ReleveKilometrage suivant = null;

        for (ReleveKilometrage existant : existants) {
            if (candidat.getId() != 0 && existant.getId() == candidat.getId()) {
                continue;
            }
            if (existant.getDateReleve().equals(candidat.getDateReleve())
                    && existant.getKilometrage() == candidat.getKilometrage()) {
                return "Un relevé identique existe déjà à cette date.";
            }

            int position = comparerAvecCandidat(existant, candidat);
            if (position < 0) {
                precedent = plusRecent(precedent, existant);
            } else if (position > 0) {
                suivant = plusAncien(suivant, existant);
            }
        }

        if (precedent != null && candidat.getKilometrage() < precedent.getKilometrage()) {
            return "Le compteur ne peut pas reculer : le relevé du " + date(precedent)
                    + " indique déjà " + kilometrage(precedent.getKilometrage()) + " km.";
        }
        if (suivant != null && candidat.getKilometrage() > suivant.getKilometrage()) {
            return "Le compteur ne peut pas dépasser le relevé suivant du " + date(suivant)
                    + " (" + kilometrage(suivant.getKilometrage()) + " km).";
        }
        return null;
    }

    /**
     * Compare un relevé existant au candidat. Une création ({@code id = 0}) se place après les
     * relevés déjà saisis le même jour, car elle recevra un identifiant plus grand.
     */
    private static int comparerAvecCandidat(ReleveKilometrage existant, ReleveKilometrage candidat) {
        int comparaison = existant.getDateReleve().compareTo(candidat.getDateReleve());
        if (comparaison != 0) return comparaison;
        if (candidat.getId() == 0) return -1;
        return Integer.compare(existant.getId(), candidat.getId());
    }

    private static int comparer(ReleveKilometrage a, ReleveKilometrage b) {
        int comparaison = a.getDateReleve().compareTo(b.getDateReleve());
        return comparaison != 0 ? comparaison : Integer.compare(a.getId(), b.getId());
    }

    private static ReleveKilometrage plusRecent(ReleveKilometrage actuel, ReleveKilometrage candidat) {
        return actuel == null || comparer(candidat, actuel) > 0 ? candidat : actuel;
    }

    private static ReleveKilometrage plusAncien(ReleveKilometrage actuel, ReleveKilometrage candidat) {
        return actuel == null || comparer(candidat, actuel) < 0 ? candidat : actuel;
    }

    private static String date(ReleveKilometrage releve) {
        return releve.getDateReleve().format(DATE_LONGUE);
    }

    private static String kilometrage(int valeur) {
        return String.format(Locale.FRANCE, "%,d", valeur).replace('\u202F', '\u00A0');
    }
}
