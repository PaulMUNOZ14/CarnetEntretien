package com.carnetentretien.carnetentretien.service;

import com.carnetentretien.carnetentretien.model.ControleTechnique;
import com.carnetentretien.carnetentretien.model.Entretien;
import com.carnetentretien.carnetentretien.model.EntretienPlanifie;
import com.carnetentretien.carnetentretien.model.ReleveKilometrage;
import com.carnetentretien.carnetentretien.model.Vehicule;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Écriture du fichier CSV d'export d'un véhicule. Séparée de la fenêtre « Enregistrer sous »
 * pour pouvoir être testée.
 */
public final class CsvHistorique {

    private CsvHistorique() {
    }

    /**
     * Écrit un export complet : relevés kilométriques, entretiens réalisés, entretiens planifiés,
     * contrôles techniques et échéances à surveiller.
     */
    public static void ecrire(File fichier, Vehicule vehicule,
                              List<ReleveKilometrage> releves,
                              List<Entretien> historique,
                              List<EntretienPlanifie> planifies,
                              List<ControleTechnique> controles,
                              List<CalculateurEntretien.Bilan> bilans,
                              ControleTechniqueService.Suivi suiviCt) throws IOException {
        try (PrintWriter writer = new PrintWriter(fichier, StandardCharsets.UTF_8)) {
            writer.write('\ufeff');

            // Véhicule
            writer.println("Véhicule :;" + echapper(nomVehicule(vehicule)));
            String immat = vehicule.getImmatriculation() == null ? "" : vehicule.getImmatriculation();
            writer.println("Immatriculation :;" + echapper(immat));
            writer.println("Kilométrage actuel :;" + vehicule.getKilometrageActuel() + " km");
            writer.println("Kilométrage annuel estimé :;" + vehicule.getKilometrageAnnuelEstime() + " km/an");
            writer.println("Date d'export :;" + LocalDate.now());
            writer.println();

            // Historique kilométrique
            writer.println("HISTORIQUE KILOMÉTRIQUE");
            writer.println("Date;Kilométrage;Variation;Remarque");
            List<ReleveKilometrage> km = releves.stream()
                    .sorted(Comparator.comparing(ReleveKilometrage::getDateReleve)
                            .thenComparingInt(ReleveKilometrage::getId)
                            .reversed())
                    .toList();
            for (int i = 0; i < km.size(); i++) {
                ReleveKilometrage r = km.get(i);
                String variation = "";
                if (i + 1 < km.size()) {
                    int delta = r.getKilometrage() - km.get(i + 1).getKilometrage();
                    variation = delta > 0 ? "+" + delta : String.valueOf(delta);
                }
                writer.println(r.getDateReleve() + ";" + r.getKilometrage() + ";"
                        + variation + ";" + echapper(r.getNote()));
            }
            if (km.isEmpty()) writer.println("Aucun");
            writer.println();

            // Entretiens réalisés
            writer.println("ENTRETIENS RÉALISÉS");
            writer.println("Date;Opération;Kilométrage;Coût (€)");
            double coutTotal = 0;
            List<Entretien> histo = historique.stream()
                    .sorted(Comparator.comparing(Entretien::getDateRealisation).reversed()
                            .thenComparingInt(Entretien::getId).reversed())
                    .toList();
            for (Entretien e : histo) {
                writer.println(e.getDateRealisation() + ";" + echapper(e.getTypeOperation()) + ";"
                        + e.getKilometrageRealisation() + ";" + e.getCout());
                coutTotal += e.getCout();
            }
            writer.println(";;Total :;" + coutTotal);
            writer.println();

            // Entretiens planifiés
            writer.println("ENTRETIENS PLANIFIÉS");
            writer.println("Date prévue;Opération;Remarque");
            List<EntretienPlanifie> plans = planifies.stream()
                    .sorted(Comparator.comparing(EntretienPlanifie::getDatePrevue)
                            .thenComparingInt(EntretienPlanifie::getId))
                    .toList();
            for (EntretienPlanifie p : plans) {
                writer.println(p.getDatePrevue() + ";" + echapper(p.getTypeOperation()) + ";"
                        + echapper(p.getNote()));
            }
            if (plans.isEmpty()) writer.println("Aucun");
            writer.println();

            // Contrôles techniques
            writer.println("CONTRÔLES TECHNIQUES");
            writer.println("Date du contrôle;Résultat;Prochain contrôle;Remarque");
            List<ControleTechnique> cts = controles.stream()
                    .sorted(Comparator.comparing(ControleTechnique::getDateRealisation).reversed()
                            .thenComparingInt(ControleTechnique::getId).reversed())
                    .toList();
            for (ControleTechnique c : cts) {
                writer.println(c.getDateRealisation() + ";" + echapper(c.getResultat().getLibelle()) + ";"
                        + c.getDateProchaine() + ";" + echapper(c.getNote()));
            }
            if (cts.isEmpty()) writer.println("Aucun");
            writer.println();

            // À surveiller (échéances)
            writer.println("À SURVEILLER");
            writer.println("Type;Élément;Échéance;État;Détail");
            for (CalculateurEntretien.Bilan b : bilans) {
                String echeance = b.dateEcheance == null ? "" : b.dateEcheance.toString();
                String etat = b.dateEcheance == null ? "Sans historique"
                        : b.depasse ? "En retard" : b.pourcentage >= 80 ? "À prévoir" : "OK";
                writer.println("Entretien;" + echapper(b.piece) + ";" + echeance + ";"
                        + echapper(etat) + ";" + echapper(aplatir(b.details)));
            }
            if (suiviCt != null && suiviCt.getEtat() != ControleTechniqueService.Etat.AUCUN) {
                String etat = suiviCt.getEtat().getLibelle();
                writer.println("Contrôle technique;" + echapper(ControleTechniqueService.OPERATION) + ";"
                        + suiviCt.getDateProchaine() + ";" + echapper(etat) + ";"
                        + echapper("Dernier contrôle : " + suiviCt.getDernier().getResultat().getLibelle()));
            }
        }
    }

    /**
     * Ancienne signature, conservée pour les appels qui ne fournissent pas l'historique
     * kilométrique. Le fichier produit ne contient alors aucun relevé.
     */
    public static void ecrire(File fichier, Vehicule vehicule,
                              List<Entretien> historique,
                              List<EntretienPlanifie> planifies,
                              List<ControleTechnique> controles,
                              List<CalculateurEntretien.Bilan> bilans,
                              ControleTechniqueService.Suivi suiviCt) throws IOException {
        ecrire(fichier, vehicule, List.of(), historique, planifies, controles, bilans, suiviCt);
    }

    private static String nomVehicule(Vehicule vehicule) {
        return vehicule.getMarque() + " " + vehicule.getModele();
    }

    private static String echapper(String texte) {
        if (texte == null) return "";
        // Point-virgule et retours à la ligne casseraient la structure CSV.
        return texte.replace(';', ',').replace("\r", " ").replace("\n", " ").trim();
    }

    private static String aplatir(String texte) {
        return echapper(texte);
    }
}
