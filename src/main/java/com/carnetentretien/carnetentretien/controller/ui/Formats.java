package com.carnetentretien.carnetentretien.controller.ui;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

/** Petites fonctions de lecture et d'écriture des nombres et des dates utilisées par l'interface. */
public final class Formats {

    /** Format long français, par exemple « 12 mars 2026 ». */
    public static final DateTimeFormatter DATE_LONGUE =
            DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Locale.FRANCE);

    private Formats() {
    }

    /** 45000 → « 45 000 ». */
    public static String km(int valeur) {
        return String.format(Locale.FRANCE, "%,d", valeur).replace('\u202F', '\u00A0');
    }

    /** @return {@code true} si le texte ne contient qu'un nombre entier (espaces ignorés) */
    public static boolean estEntier(String s) {
        return sansEspaces(s).matches("\\d{1,9}");
    }

    /** @throws NumberFormatException si le texte n'est pas un entier valide */
    public static int parseEntier(String s) {
        return Integer.parseInt(sansEspaces(s));
    }

    /** @return {@code true} si le texte est vide ou représente un montant (virgule ou point acceptés) */
    public static boolean estMontant(String s) {
        String propre = sansEspaces(s).replace("€", "").replace(',', '.');
        return propre.isEmpty() || propre.matches("\\d{1,7}(\\.\\d{1,2})?");
    }

    /** @return le montant saisi, ou {@code 0} si la case est vide */
    public static double parseMontant(String s) {
        String propre = sansEspaces(s).replace("€", "").replace(',', '.');
        return propre.isEmpty() ? 0 : Double.parseDouble(propre);
    }

    private static String sansEspaces(String s) {
        return s == null ? "" : s.replaceAll("[\\s\\u00A0\\u202F]", "");
    }
}
