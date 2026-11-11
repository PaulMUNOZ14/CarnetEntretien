package com.carnetentretien.carnetentretien.controller.ui;

import com.carnetentretien.carnetentretien.db.EntretienDB;
import com.carnetentretien.carnetentretien.db.PreconisationDB;
import com.carnetentretien.carnetentretien.model.Entretien;
import com.carnetentretien.carnetentretien.model.Preconisation;
import com.carnetentretien.carnetentretien.model.Vehicule;
import com.carnetentretien.carnetentretien.service.CalculateurEntretien;
import javafx.scene.control.Label;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Niveaux d'état d'un entretien et fabrication des badges colorés correspondants. */
public final class EntretienUi {

    public static final int OK = 0;
    public static final int PREVOIR = 1;
    public static final int URGENT = 2;
    public static final int INCONNU = 3;
    public static final int AUCUNE_REGLE = -1;

    private EntretienUi() {
    }

    /** Analyse un véhicule avec ses règles ; les entretiens les plus usés sont placés en premier. */
    public static List<CalculateurEntretien.Bilan> analyser(Vehicule v) {
        List<Entretien> historique = EntretienDB.getEntretiensPourVehicule(v.getId());
        List<Preconisation> regles = PreconisationDB.getToutesLesPreconisations();
        List<CalculateurEntretien.Bilan> bilans =
                new ArrayList<>(CalculateurEntretien.analyserEntretien(v, historique, regles));
        bilans.sort(Comparator.comparingDouble((CalculateurEntretien.Bilan b) -> b.pourcentage).reversed());
        return bilans;
    }

    /** @return le niveau d'état correspondant à un pourcentage d'usure */
    public static int niveauDe(double pourcentage) {
        if (pourcentage >= 100) return URGENT;
        if (pourcentage >= 80) return PREVOIR;
        if (pourcentage > 0) return OK;
        return INCONNU;
    }

    /** @return le pire niveau parmi les bilans, ou {@link #AUCUNE_REGLE} si la liste est vide */
    public static int niveauGlobal(List<CalculateurEntretien.Bilan> bilans) {
        if (bilans.isEmpty()) return AUCUNE_REGLE;
        boolean urgent = false, prevoir = false, ok = false;
        for (CalculateurEntretien.Bilan b : bilans) {
            int n = niveauDe(b.pourcentage);
            if (n == URGENT) urgent = true;
            else if (n == PREVOIR) prevoir = true;
            else if (n == OK) ok = true;
        }
        if (urgent) return URGENT;
        if (prevoir) return PREVOIR;
        if (ok) return OK;
        return INCONNU;
    }

    /** @return le suffixe de classe CSS correspondant à un niveau ({@code ok}, {@code prevoir}, {@code urgent}, {@code neutre}) */
    public static String suffixe(int niveau) {
        return switch (niveau) {
            case OK -> "ok";
            case PREVOIR -> "prevoir";
            case URGENT -> "urgent";
            default -> "neutre";
        };
    }

    /** Construit le badge coloré affichant l'état d'un entretien. */
    public static Label creerBadge(int niveau) {
        String texte = switch (niveau) {
            case OK -> "✔  Tout va bien";
            case PREVOIR -> "⏰  À prévoir bientôt";
            case URGENT -> "⚠  À faire maintenant";
            case INCONNU -> "Pas encore d'information";
            default -> "Aucun entretien surveillé";
        };
        Label badge = new Label(texte);
        badge.getStyleClass().addAll("badge", "badge-" + suffixe(niveau));
        return badge;
    }
}
