package com.carnetentretien.carnetentretien.db;

import com.carnetentretien.carnetentretien.BaseBddTest;
import com.carnetentretien.carnetentretien.model.ReleveKilometrage;
import com.carnetentretien.carnetentretien.model.Vehicule;
import com.carnetentretien.carnetentretien.service.AppPaths;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseManagerTest extends BaseBddTest {

    private List<String> valeurs(String requete, String colonne) throws SQLException {
        List<String> resultat = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(requete)) {
            while (rs.next()) {
                resultat.add(rs.getString(colonne));
            }
        }
        return resultat;
    }

    @Test
    void connectOuvreUneConnexionValide() throws SQLException {
        try (Connection conn = DatabaseManager.connect()) {
            assertNotNull(conn);
            assertTrue(conn.isValid(2));
        }
    }

    @Test
    void laBaseEstCreeeDansLeDossierDeDonnees() {
        assertTrue(Files.isRegularFile(AppPaths.fichierBaseDeDonnees()));
    }

    @Test
    void initDatabaseCreeLesTablesAttendues() throws SQLException {
        List<String> tables = valeurs("SELECT name FROM sqlite_master WHERE type = 'table'", "name");

        assertTrue(tables.contains("Vehicule"));
        assertTrue(tables.contains("Entretien_Fait"));
        assertTrue(tables.contains("Preconisation"));
        assertTrue(tables.contains("Entretien_Planifie"));
        assertTrue(tables.contains("ControleTechnique"));
        assertTrue(tables.contains("ControleTechnique_Planifie"));
        assertTrue(tables.contains("Releve_Kilometrage"));
    }

    @Test
    void laTableReleveKilometrageAToutesSesColonnes() throws SQLException {
        List<String> colonnes = valeurs("PRAGMA table_info(Releve_Kilometrage)", "name");

        for (String attendue : List.of("vehicule_id", "date_releve", "kilometrage", "note")) {
            assertTrue(colonnes.contains(attendue), "colonne manquante : " + attendue);
        }
    }

    @Test
    void laTableVehiculeAToutesSesColonnes() throws SQLException {
        List<String> colonnes = valeurs("PRAGMA table_info(Vehicule)", "name");

        for (String attendue : List.of("immatriculation", "marque", "modele", "kilometrage_actuel",
                "kilometrage_annuel_estime", "image_url", "image_locale")) {
            assertTrue(colonnes.contains(attendue), "colonne manquante : " + attendue);
        }
    }

    @Test
    void initDatabaseEstRepetableSansPerdreLesDonnees() {
        VehiculeDB.ajouterVehicule(new Vehicule(0, "AB-123-CD", "Peugeot", "208", 1000, 2000));

        DatabaseManager.initDatabase();
        DatabaseManager.initDatabase();

        assertEquals(1, VehiculeDB.getTousLesVehicules().size());
    }

    @Test
    void unVehiculeSansReleveRecoitUnReleveInitialQuandLaTableEstCreee() throws SQLException {
        try (Connection conn = DatabaseManager.connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE Releve_Kilometrage");
            stmt.execute("INSERT INTO Vehicule(marque, modele, kilometrage_actuel, kilometrage_annuel_estime) "
                    + "VALUES('Renault','Clio',80000,10000)");
        }

        DatabaseManager.initDatabase();

        Vehicule v = VehiculeDB.getTousLesVehicules().get(0);
        List<ReleveKilometrage> releves = ReleveKilometrageDB.getRelevesPourVehicule(v.getId());
        assertEquals(1, releves.size());
        assertEquals(80000, releves.get(0).getKilometrage());
        assertEquals(LocalDate.now(), releves.get(0).getDateReleve());
        assertEquals("Relevé initial", releves.get(0).getNote());

        DatabaseManager.initDatabase();
        assertEquals(1, ReleveKilometrageDB.getRelevesPourVehicule(v.getId()).size());
    }

    @Test
    void uneAncienneBaseSansLesColonnesRecentesEstMigreeSansPerdreSesVehicules() throws SQLException {
        // Base d'une version précédente de l'application : ni immatriculation, ni image_url, ni image_locale
        try (Connection conn = DatabaseManager.connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE Vehicule");
            stmt.execute("CREATE TABLE Vehicule (id INTEGER PRIMARY KEY AUTOINCREMENT, marque TEXT NOT NULL, "
                    + "modele TEXT NOT NULL, kilometrage_actuel INTEGER NOT NULL, kilometrage_annuel_estime INTEGER NOT NULL)");
            stmt.execute("INSERT INTO Vehicule(marque, modele, kilometrage_actuel, kilometrage_annuel_estime) "
                    + "VALUES('Renault','Clio',80000,10000)");
        }

        DatabaseManager.initDatabase();

        List<String> colonnes = valeurs("PRAGMA table_info(Vehicule)", "name");
        assertTrue(colonnes.contains("immatriculation"));
        assertTrue(colonnes.contains("image_url"));
        assertTrue(colonnes.contains("image_locale"));
        List<Vehicule> vehicules = VehiculeDB.getTousLesVehicules();
        assertEquals(1, vehicules.size());
        assertEquals("Clio", vehicules.get(0).getModele());
        assertNull(vehicules.get(0).getImmatriculation());
        assertNull(vehicules.get(0).getImageUrl());
        assertNull(vehicules.get(0).getImageLocale());
    }

    @Test
    void leDossierDeDonneesManquantEstCreeAuPremierLancement() throws Exception {
        Files.delete(AppPaths.fichierBaseDeDonnees());   // supprime la base créée par le test de base
        Files.delete(dossier);                            // le dossier n'existe plus (premier lancement)

        DatabaseManager.initDatabase();

        assertTrue(Files.isRegularFile(AppPaths.fichierBaseDeDonnees()));
    }
}
