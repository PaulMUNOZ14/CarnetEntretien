package com.carnetentretien.carnetentretien.controller.ui;

import com.carnetentretien.carnetentretien.db.ControleTechniqueDB;
import com.carnetentretien.carnetentretien.db.EntretienDB;
import com.carnetentretien.carnetentretien.db.PreconisationDB;
import com.carnetentretien.carnetentretien.model.ControleTechnique;
import com.carnetentretien.carnetentretien.model.Entretien;
import com.carnetentretien.carnetentretien.model.Preconisation;
import com.carnetentretien.carnetentretien.model.Vehicule;
import com.carnetentretien.carnetentretien.service.CalculateurEntretien;
import com.carnetentretien.carnetentretien.service.ControleTechniqueService;
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
        bilans.sort(Comparator.comparingInt((CalculateurEntretien.Bilan b) -> priorite(b))
                .reversed()
                .thenComparing(Comparator.comparingDouble((CalculateurEntretien.Bilan b) -> b.pourcentage).reversed())
                .thenComparing(b -> b.piece, String.CASE_INSENSITIVE_ORDER));
        return bilans;
    }

    /** Priorité d'affichage : urgents, puis à prévoir, puis OK, et en dernier sans information. */
    private static int priorite(CalculateurEntretien.Bilan b) {
        return switch (niveauDe(b)) {
            case URGENT -> 3;
            case PREVOIR -> 2;
            case OK -> 1;
            case INCONNU -> 0;
            default -> -1;
        };
    }

    /** @return le niveau d'état correspondant à un pourcentage d'usure */
    public static int niveauDe(double pourcentage) {
        if (pourcentage >= 100) return URGENT;
        if (pourcentage >= 80) return PREVOIR;
        return OK;
    }

    /**
     * @return le niveau d'état d'un bilan : {@link #INCONNU} si aucun historique ne permet de
     * calculer l'échéance, sinon le niveau déduit de l'usure (0 % = tout juste fait = OK).
     */
    public static int niveauDe(CalculateurEntretien.Bilan bilan) {
        if (bilan.dateEcheance == null) return INCONNU;
        return niveauDe(bilan.pourcentage);
    }

    /** @return le pire niveau parmi les bilans, ou {@link #AUCUNE_REGLE} si la liste est vide */
    public static int niveauGlobal(List<CalculateurEntretien.Bilan> bilans) {
        if (bilans.isEmpty()) return AUCUNE_REGLE;
        boolean urgent = false, prevoir = false, ok = false, inconnu = false;
        for (CalculateurEntretien.Bilan b : bilans) {
            int n = niveauDe(b);
            if (n == URGENT) urgent = true;
            else if (n == PREVOIR) prevoir = true;
            else if (n == OK) ok = true;
            else if (n == INCONNU) inconnu = true;
        }
        if (urgent) return URGENT;
        if (prevoir) return PREVOIR;
        if (ok) return OK;
        if (inconnu) return INCONNU;
        return AUCUNE_REGLE;
    }

    /**
     * @return le pire niveau d'un véhicule en combinant les entretiens suivis et le contrôle
     * technique (dépassement = urgent, à prévoir dans les 2 mois = à prévoir).
     */
    public static int niveauGlobal(Vehicule v) {
        int niveau = niveauGlobal(analyser(v));
        List<ControleTechnique> controles = ControleTechniqueDB.getControlesPourVehicule(v.getId());
        ControleTechniqueService.Suivi suivi = ControleTechniqueService.analyser(controles);
        return combinerAvecControle(niveau, suivi);
    }

    /** Combine un niveau d'entretien avec l'état du contrôle technique. */
    public static int combinerAvecControle(int niveauEntretiens, ControleTechniqueService.Suivi suiviCt) {
        if (suiviCt == null) return niveauEntretiens;
        boolean ctUrgent = suiviCt.getEtat() == ControleTechniqueService.Etat.EN_RETARD;
        boolean ctPrevoir = suiviCt.getEtat() == ControleTechniqueService.Etat.A_PREVOIR;
        boolean ctOk = suiviCt.getEtat() == ControleTechniqueService.Etat.OK;
        if (niveauEntretiens == URGENT || ctUrgent) return URGENT;
        if (niveauEntretiens == PREVOIR || ctPrevoir) return PREVOIR;
        if (niveauEntretiens == OK || ctOk) return OK;
        if (niveauEntretiens == INCONNU) return INCONNU;
        return AUCUNE_REGLE;
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
