package com.carnetentretien.carnetentretien;

import com.carnetentretien.carnetentretien.db.DatabaseManager;
import org.junit.jupiter.api.BeforeEach;

/** Base des tests de la couche base de données : une base SQLite vierge est créée avant chaque test. */
public abstract class BaseBddTest extends BaseDonneesTest {

    @BeforeEach
    void creerBaseVierge() {
        DatabaseManager.initDatabase();
    }
}
