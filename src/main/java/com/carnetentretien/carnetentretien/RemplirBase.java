package com.carnetentretien.carnetentretien;

import com.carnetentretien.carnetentretien.db.ControleTechniqueDB;
import com.carnetentretien.carnetentretien.db.DatabaseManager;
import com.carnetentretien.carnetentretien.db.EntretienDB;
import com.carnetentretien.carnetentretien.db.EntretienPlanifieDB;
import com.carnetentretien.carnetentretien.db.PreconisationDB;
import com.carnetentretien.carnetentretien.db.VehiculeDB;
import com.carnetentretien.carnetentretien.model.ControleTechnique;
import com.carnetentretien.carnetentretien.model.Entretien;
import com.carnetentretien.carnetentretien.model.EntretienPlanifie;
import com.carnetentretien.carnetentretien.model.Preconisation;
import com.carnetentretien.carnetentretien.model.Vehicule;

import java.time.LocalDate;
import java.util.List;

/**
 * Utilitaire de développement : remplit la base de données locale avec des données réalistes
 * pour tester l'application (véhicules, historiques, planifications, contrôles techniques).
 * À lancer depuis la racine du projet.
 */
public final class RemplirBase {

    private RemplirBase() {
    }

    public static void main(String[] args) {
        DatabaseManager.initDatabase();
        vider();

        // Règles de suivi
        ajouterRegle("Vidange", 15000, 12);
        ajouterRegle("Filtre à huile", 15000, 12);
        ajouterRegle("Filtre à air", 30000, 24);
        ajouterRegle("Filtre d'habitacle", 15000, 12);
        ajouterRegle("Plaquettes de frein avant", 30000, 36);
        ajouterRegle("Plaquettes de frein arrière", 50000, 60);
        ajouterRegle("Pneus", 40000, 48);
        ajouterRegle("Courroie de distribution", 120000, 72);
        ajouterRegle("Liquide de frein", 60000, 24);

        int peugeot = ajouterVehicule("AB-123-CD", "Peugeot", "208", 78500, 12000);
        int clio = ajouterVehicule("EF-456-GH", "Renault", "Clio IV", 142300, 15000);
        int citroen = ajouterVehicule(null, "Citroën", "C3", 32100, 8000);

        remplirPeugeot(peugeot);
        remplirClio(clio);
        remplirCitroen(citroen);

        List<Vehicule> vehicules = VehiculeDB.getTousLesVehicules();
        System.out.println("Base de démonstration prête : " + vehicules.size() + " véhicule(s).");
        for (Vehicule v : vehicules) {
            int entretiens = EntretienDB.getEntretiensPourVehicule(v.getId()).size();
            int planifies = EntretienPlanifieDB.getEntretiensPlanifiesPourVehicule(v.getId()).size();
            int controles = ControleTechniqueDB.getControlesPourVehicule(v.getId()).size();
            String immat = v.getImmatriculation() == null ? "" : " (" + v.getImmatriculation() + ")";
            System.out.println(" - " + v.getMarque() + " " + v.getModele() + immat
                    + " : " + entretiens + " entretien(s), " + planifies + " planifié(s), " + controles + " CT");
        }
    }

    private static void vider() {
        String[] tables = {
                "ControleTechnique", "Entretien_Planifie", "Entretien_Fait", "Preconisation", "Vehicule"
        };
        try (var conn = DatabaseManager.connect();
             var stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = OFF");
            for (String t : tables) {
                stmt.execute("DELETE FROM " + t);
            }
            stmt.execute("PRAGMA foreign_keys = ON");
        } catch (Exception e) {
            System.err.println("Impossible de vider la base : " + e.getMessage());
        }
    }

    private static int ajouterVehicule(String immat, String marque, String modele, int km, int kmAn) {
        VehiculeDB.ajouterVehicule(new Vehicule(0, immat, marque, modele, km, kmAn));
        List<Vehicule> tous = VehiculeDB.getTousLesVehicules();
        return tous.get(tous.size() - 1).getId();
    }

    private static void ajouterRegle(String nom, int km, int mois) {
        PreconisationDB.ajouterPreconisation(new Preconisation(0, nom, km, mois));
    }

    private static void entretien(int vid, String type, LocalDate date, int km, double cout) {
        EntretienDB.ajouterEntretien(new Entretien(0, vid, type, date, km, cout));
    }

    private static void planifie(int vid, String type, LocalDate date, String note) {
        EntretienPlanifieDB.ajouterEntretienPlanifie(new EntretienPlanifie(0, vid, type, date, note));
    }

    private static void ct(int vid, LocalDate date, ControleTechnique.Resultat resultat, String note) {
        LocalDate prochaine = resultat.dateProchaine(date);
        ControleTechniqueDB.ajouterControleTechnique(
                new ControleTechnique(0, vid, date, resultat, prochaine, note));
    }

    private static void remplirPeugeot(int vid) {
        LocalDate auj = LocalDate.now();
        // Historique varié
        entretien(vid, "Vidange", auj.minusMonths(18), 62000, 89.90);
        entretien(vid, "Vidange", auj.minusMonths(6), 73500, 95.00);
        entretien(vid, "Filtre à air", auj.minusMonths(14), 64500, 35.00);
        entretien(vid, "Pneus", auj.minusMonths(30), 48000, 320.00);
        entretien(vid, "Plaquettes de frein avant", auj.minusMonths(10), 70000, 180.00);
        entretien(vid, "Filtre d'habitacle", auj.minusMonths(6), 73500, 22.50);
        // Planifiés
        planifie(vid, "Vidange", auj.plusDays(20), "Garage du centre, prévoir filtre");
        planifie(vid, "Courroie de distribution", auj.plusMonths(3), "Devis demandé");
        // CT favorable récent -> à jour
        ct(vid, auj.minusMonths(8), ControleTechnique.Resultat.FAVORABLE, "RAS");
    }

    private static void remplirClio(int vid) {
        LocalDate auj = LocalDate.now();
        // Historique ancien / en retard sur certaines opérations
        entretien(vid, "Vidange", auj.minusMonths(20), 118000, 79.00);
        entretien(vid, "Filtre à air", auj.minusMonths(40), 98000, 28.00);
        entretien(vid, "Pneus", auj.minusMonths(50), 90000, 280.00);
        entretien(vid, "Liquide de frein", auj.minusMonths(36), 110000, 60.00);
        // Planifié
        planifie(vid, "Vidange", auj.plusDays(5), "Urgent, rappel garage");
        // CT défavorable récent -> contre-visite proche / à prévoir
        ct(vid, auj.minusDays(25), ControleTechnique.Resultat.DEFAVORABLE, "Feux de stop à réparer");
        // Ancien CT
        ct(vid, auj.minusYears(2).minusMonths(1), ControleTechnique.Resultat.FAVORABLE, null);
    }

    private static void remplirCitroen(int vid) {
        LocalDate auj = LocalDate.now();
        entretien(vid, "Vidange", auj.minusMonths(2), 30500, 85.00);
        entretien(vid, "Filtre d'habitacle", auj.minusMonths(2), 30500, 18.00);
        // CT ancien -> échéance dépassée (en retard)
        ct(vid, auj.minusYears(2).minusMonths(3), ControleTechnique.Resultat.FAVORABLE, "Contrôle OK");
        // Pas de planifié pour montrer un véhicule "à jour" côté entretiens mais CT en retard
    }
}
